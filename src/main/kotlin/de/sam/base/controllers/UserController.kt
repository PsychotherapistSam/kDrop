package de.sam.base.controllers

import de.sam.base.authentication.PasswordHasher
import de.sam.base.authentication.UserService
import de.sam.base.authentication.UserValidator
import de.sam.base.database.UserDAO
import de.sam.base.database.UserDTO
import de.sam.base.database.fetchDAO
import de.sam.base.database.toDTO
import de.sam.base.users.UserRoles
import de.sam.base.utils.CacheInvalidation
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.logging.logTimeSpent
import de.sam.base.utils.preferences.Preferences
import io.javalin.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.system.measureNanoTime

class UserController : KoinComponent {
    private val userValidatorNew: UserValidator by inject()
    private val userService: UserService by inject()
    private val passwordHasher: PasswordHasher by inject()
    fun updateUser(ctx: Context) {
        val selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")!!
        /*if (ctx.currentUser != selectedUser && !ctx.currentUser!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to update this user")
        }*/

        val isSelf = ctx.currentUserDTO!!.id == selectedUserDTO.id

        transaction {
            val user = selectedUserDTO.fetchDAO() ?: throw NotFoundResponse("User not found")
            ctx.formParamMap().forEach { (key, value) ->
                when (key) {
                    "username" -> {
                        val newName = value.first()
                        //TODO: move all this to the service or something
                        if (selectedUserDTO.name.lowercase() != newName.lowercase()) {
                            val (isValid, errors) = userValidatorNew.validateUsername(newName)
                            if (!isValid)
                                throw BadRequestResponse(errors.first())

                            if (userService.getUserByUsername(newName) != null)
                                throw BadRequestResponse("Username already taken")

                            user.name = newName
                        }
                    }

                    "password" -> {
                        val newPassword = value.first()

                        val (isValid, errors) = userValidatorNew.validatePassword(newPassword)
                        if (!isValid)
                            throw BadRequestResponse(errors.first())

                        val salt = UUID.randomUUID().toString()
                        val hashedPassword = passwordHasher.hashPassword(newPassword, salt)

                        user.salt = salt
                        user.password = hashedPassword

                    }

                    "roles" -> {
                        if (ctx.currentUserDTO!!.hasRolePowerLevel(UserRoles.ADMIN)) {
                            val (isValid, errors) = userValidatorNew.validateRoles(value.first())
                            if (!isValid)
                                throw BadRequestResponse(errors.first())

                            val roles = value.first().split(",").map { UserRoles.valueOf(it) }

                            if (roles != selectedUserDTO.roles) {
                                user.roles = roles.joinToString(",") { it.name }
                            }
                        }
                    }

                    else -> {
                        if (ctx.isLoggedIn) {
                            if (Preferences.preferencesList.any { it.first == key && it.second == Boolean }) {
                                val preferences =
                                    user.preferences.split(",").filter { it.isNotBlank() }.distinct().toMutableList()
                                if (value.first().toBoolean()) {
                                    preferences.add(key)
                                } else {
                                    preferences.remove(key)
                                }
                                user.preferences = preferences.joinToString(",")
                            }
                        }
                    }
                }
                // updates the user in the current session (e.g. updating username etc), but only when it is not done by an admin
                if (isSelf) {
                    ctx.currentUserDTO = user.toDTO()
                }
                CacheInvalidation.userTokens[selectedUserDTO.id] = System.currentTimeMillis()
            }
        }
    }

    fun deleteUser(ctx: Context) {
        val selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")!!

        if (ctx.currentUserDTO != selectedUserDTO && !ctx.currentUserDTO!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to delete this user")
        }

        userService.deleteUser(selectedUserDTO.id)
        CacheInvalidation.userTokens[selectedUserDTO.id] = System.currentTimeMillis()
    }

    fun getUserParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("userId")
                .check({
                    transaction {
                        logTimeSpent("Getting user by id") {
                            val userDao = UserDAO.findById(it)
                            if (userDao != null) {
                                ctx.attribute("requestUserParameter", userDao.toDTO())
                                return@transaction true
                            } else {
                                return@transaction false
                            }
                        }
                    }
                }, "User ID is not valid")
                .get()
        }
        ctx.attribute("userQueryTime", userQueryTime)
    }
}

