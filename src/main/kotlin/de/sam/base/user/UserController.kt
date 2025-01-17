package de.sam.base.user

import de.sam.base.authentication.PasswordHasher
import de.sam.base.database.UserDTO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.logging.logTimeSpent
import de.sam.base.utils.preferences.Preferences
import de.sam.base.utils.session.Session
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.pathParamAsClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.system.measureNanoTime

class UserController : KoinComponent {
    private val userValidatorNew: UserValidator by inject()
    private val userRepository: UserRepository by inject()
    private val passwordHasher: PasswordHasher by inject()
    private val session: Session by inject()

    fun updateUser(ctx: Context) {
        val selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")!!

        val isSelf = ctx.currentUserDTO!!.id == selectedUserDTO.id

        val newUser = selectedUserDTO.copy()

        ctx.formParamMap().forEach { (key, value) ->
            when (key) {
                "username" -> {
                    val newName = value.first()
                    //TODO: move all this to the service or something
                    if (selectedUserDTO.name.lowercase() != newName.lowercase()) {
                        val (isValid, errors) = userValidatorNew.validateUsername(newName)
                        if (!isValid)
                            throw BadRequestResponse(errors.first())

                        if (userRepository.getUserByUsername(newName) != null)
                            throw BadRequestResponse("Username already taken")

                        newUser.name = newName
                    }
                }

                "password" -> {
                    val newPassword = value.first()

                    val (isValid, errors) = userValidatorNew.validatePassword(newPassword)
                    if (!isValid)
                        throw BadRequestResponse(errors.first())

                    val salt = UUID.randomUUID().toString()
                    val hashedPassword = passwordHasher.hashPassword(newPassword, salt)

                    newUser.salt = salt
                    newUser.password = hashedPassword
                }

                "roles" -> {
                    if (ctx.currentUserDTO!!.hasRolePowerLevel(UserRoles.ADMIN)) {
                        val (isValid, errors) = userValidatorNew.validateRoles(value.first())
                        if (!isValid)
                            throw BadRequestResponse(errors.first())

                        val roles = value.first().split(",").map { UserRoles.valueOf(it) }

                        if (roles != selectedUserDTO.roles) {
                            newUser.roles = roles
                        }
                    }
                }

                else -> {
                    if (ctx.isLoggedIn) {
                        if (Preferences.preferencesList.any { it.first == key && it.second == Boolean }) {
                            val preferences =
                                selectedUserDTO.preferences.split(",").filter { it.isNotBlank() }.distinct()
                                    .toMutableList()
                            if (value.first().toBoolean()) {
                                preferences.add(key)
                            } else {
                                preferences.remove(key)
                            }
                            newUser.preferences = preferences.joinToString(",")
                        }
                    }
                }
            }

            userRepository.updateUser(newUser)

            // updates the user in the current session (e.g. updating username etc), but only when it is not done by an admin
            if (isSelf) {
                ctx.currentUserDTO = newUser
            }
            session.forceUpdateUserSessionObject(selectedUserDTO.id, newUser)
        }
    }

    fun deleteUser(ctx: Context) {
        val selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")!!

        if (ctx.currentUserDTO != selectedUserDTO && !ctx.currentUserDTO!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to delete this user")
        }
        session.invalidateAllSessions(selectedUserDTO.id)
        userRepository.deleteUser(selectedUserDTO.id)
    }

    fun getUserParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("userId")
                .check({
                    userRepository.getUserById(it)
                    logTimeSpent("Getting user by id") {
                        val user = userRepository.getUserById(it)
                        if (user != null) {
                            ctx.attribute("requestUserParameter", user)
                            true
                        } else {
                            false
                        }
                    }
                }, "User ID is not valid")
                .get()
        }
        ctx.attribute("userQueryTime", userQueryTime)
    }
}

