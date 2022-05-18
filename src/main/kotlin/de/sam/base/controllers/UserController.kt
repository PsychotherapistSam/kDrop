package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.config.Configuration
import de.sam.base.database.User
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import de.sam.base.database.toUser
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import io.javalin.core.validation.ValidationError
import io.javalin.core.validation.Validator
import io.javalin.http.*
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureNanoTime

class UserController {

    fun updateUser(ctx: Context) {
        val selectedUser = ctx.attribute<User>("requestUserParameter")!!
        /*if (ctx.currentUser != selectedUser && !ctx.currentUser!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to update this user")
        }*/

        val isSelf = ctx.currentUser!!.id == selectedUser.id

        transaction {
            val user = UserDAO.findById(selectedUser.id) ?: throw NotFoundResponse("User not found")
            ctx.formParamMap().forEach { (key, value) ->
                when (key) {
                    "username" -> {
                        val newName = value.first()
                        if (selectedUser.name.lowercase() != newName.lowercase()) {
                            val usernameErrors = validateUsername(newName, false).first
                            if (usernameErrors.isNotEmpty())
                                throw BadRequestResponse(usernameErrors.first().message) //.joinToString("\n") { it.message }
                            user.name = newName
                        }
                    }
                    "password" -> {
                        val newPassword = value.first()
                        val passwordErrors = validatePassword(null, newPassword)
                        if (passwordErrors.isNotEmpty())
                            throw BadRequestResponse(passwordErrors.first().message) //.joinToString("\n") { it.message })
                        user.password = Password.hash(newPassword)
                            .addSalt("${selectedUser.id}") // argon2id salts the passwords on itself, but better safe than sorry
                            .addPepper(Configuration.config.passwordPepper)
                            .with(argon2Instance)
                            .result
                    }
                    "roles" -> {
                        if (ctx.currentUser!!.hasRolePowerLevel(UserRoles.ADMIN)) {
                            // map either null, comma seperated list to enum list1
                            val roleValidationErrors = validateRoles(value.first())
                            if (roleValidationErrors.isNotEmpty())
                                throw BadRequestResponse(roleValidationErrors.first().message) //.joinToString("\n") { it.message })
                            val roles = value.first().split(",").map { UserRoles.valueOf(it) }
                            if (roles != selectedUser.roles) {
                                user.roles = roles.joinToString(",") { it.name }
                            }
                        }
                    }
                }
                // updates the user in the current session (e.g. updating username etc), but only when it is not done by an admin
                if (isSelf) {
                    ctx.currentUser = user.toUser()
                }
            }
        }
    }

    fun deleteUser(ctx: Context) {
        val selectedUser = ctx.attribute<User>("requestUserParameter")!!

        if (ctx.currentUser != selectedUser && !ctx.currentUser!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to delete this user")
        }
        transaction {
            UserDAO
                .findById(selectedUser.id)!!
                .delete()
        }
    }

    fun getUserParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("userId")
                .check({
                    transaction {
                        logTimeSpent("Getting user by id") {
                            val userDao = UserDAO.findById(it)
                            if (userDao != null) {
                                ctx.attribute("requestUserParameter", userDao.toUser())
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

fun validateUsername(
    username: String?,
    userHasToExist: Boolean = true
): Pair<ArrayList<ValidationError<String>>, User?> {
    val fieldName = "username"
    var user: User? = null

    val errors: ArrayList<ValidationError<String>> = ArrayList()

    var validator = Validator.create(String::class.java, username, fieldName)
        .check({ it.isNotBlank() }, "Username is required")
        .check({ it.length <= 20 }, "Username is too long")
        .check({ it.length >= 3 }, "Username is too short")

    if (validator.errors().isNotEmpty()) {
        errors.addAll(validator.errors()[fieldName]!!)
    } else {
        val userDao = transaction {
            addLogger(StdOutSqlLogger)
            return@transaction UserDAO.find { UsersTable.name.lowerCase() like username!!.lowercase() }
                .firstOrNull()
        }
        if (userDao != null)
            user = userDao.toUser()

        validator = if (userHasToExist) {
            Validator.create(String::class.java, username, fieldName)
                .check({ userDao != null }, "Invalid username or password")
        } else {
            Validator.create(String::class.java, username, fieldName)
                .check({ userDao == null }, "User already exists")
        }

        if (validator.errors().isNotEmpty()) {
            errors.addAll(validator.errors()[fieldName]!!)
        }
    }

    return Pair(errors, user)
}

private val argon2Instance = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)

fun validatePassword(user: User? = null, password: String?): List<ValidationError<String>> {
    val fieldName = "password"
    return Validator.create(String::class.java, password, fieldName)
        .check({ it.isNotBlank() }, "Password is required")
        .check({ it.length <= 128 }, "Password is too long")
        .check({ it.length >= 3 }, "Password is too short")
        .check(
            {
                // if no user is specified the password will only get checked for basic validity
                if (user == null) {
                    true
                } else {
                    Password.check(it, user!!.password)
                        .addSalt("${user.id}") // argon2id salts the passwords on itself, but better safe than sorry
                        .addPepper(Configuration.config.passwordPepper)
                        .with(argon2Instance)
                }
            },
            "Invalid username or password"
        ).errors().getOrElse(fieldName) { arrayListOf() }
}

fun validateRoles(roles: String): List<ValidationError<String>> {
    val fieldName = "role"
    return Validator.create(String::class.java, roles, fieldName)
        .check({ it.isNotBlank() }, "Roles must not be empty")
        .check({
            it.split(",")
                .all { splitRole -> splitRole in UserRoles.values().map { role -> role.name } }
        }, "Invalid roles")
        .errors().getOrElse(fieldName) { arrayListOf() }
}