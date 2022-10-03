package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.UserDAO
import de.sam.base.database.UserDTO
import de.sam.base.database.toDTO
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.prolongAtLeast
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.InternalServerErrorResponse
import io.javalin.validation.ValidationError
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class AuthenticationController {

    companion object {
        val argon2Instance: Argon2Function = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)
    }

    fun loginRequest(ctx: Context) {
        // hide discrepancy on whether an account exists or not to prevent account enumeration
        prolongAtLeast(2000) {
            if (ctx.isLoggedIn) {
                ctx.status(200)
                return@prolongAtLeast
            }

            val attempt = validateLoginAttempt(ctx.formParam("username"), ctx.formParam("password"))
            // first = user, second = errors
            if (attempt.second.isNotEmpty()) {
                ctx.status(HttpStatus.FORBIDDEN)
                ctx.json(attempt.second)
                return@prolongAtLeast
            }

            ctx.status(HttpStatus.OK)
            ctx.currentUserDTO = attempt.first
        }
    }

    fun registrationRequest(ctx: Context) {

        throw InternalServerErrorResponse("not yet supported")

        if (ctx.isLoggedIn && ctx.currentUserDTO!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            ctx.json("You are already registered.")
            ctx.status(HttpStatus.FORBIDDEN)
        } else if (!ctx.isLoggedIn && !config.allowUserRegistration) {
            ctx.json("User registration is currently disabled.")
            ctx.status(HttpStatus.FORBIDDEN)
        } else {
            prolongAtLeast(2000) {
                val username = ctx.formParam("username")
                val usernameErrors = validateUsername(username, false).first

                val password = ctx.formParam("password")
                val passwordErrors = validatePassword(null, password)

                val errors = arrayListOf<ValidationError<String>>()

                if (usernameErrors.isNotEmpty())
                    errors.addAll(usernameErrors)
                if (passwordErrors.isNotEmpty())
                    errors.addAll(passwordErrors)

                if (errors.isNotEmpty()) {
                    ctx.status(HttpStatus.FORBIDDEN)
                    ctx.json(errors.map { it.message })
                } else {
                    val userDAO = transaction {
                        return@transaction UserDAO.new {
                            this.name = username!!
                            this.password = Password.hash(password)
                                .addSalt("${this.id}") // argon2id salts the passwords on itself, but better safe than sorry
                                .addPepper(config.passwordPepper)
                                .with(argon2Instance)
                                .result
                            this.roles = UserRoles.USER.name
                            this.hidden = false
                            this.preferences = ""
                            this.registrationDate = DateTime.now()
                        }
                    }

                    // only set the session when it is not requested to leave it out.
                    if (ctx.header("No-Session") == null) {
                        ctx.currentUserDTO = userDAO.toDTO()
                    }
                    ctx.status(200)
                }
            }
        }
    }

    fun logoutRequest(ctx: Context) {
        ctx.req().session.invalidate()
    }
}

fun validateLoginAttempt(username: String?, password: String?): Pair<UserDTO?, List<String>> {
    val usernameValidation = validateUsername(username)
    val usernameErrors = usernameValidation.first
    val user = usernameValidation.second
    if (usernameErrors.isNotEmpty()) {
        return Pair(null, usernameErrors.map { it.message }) // don't bother with password validation
    }

    val passwordValidation = validatePassword(user, password)
    if (passwordValidation.isNotEmpty()) {
        return Pair(null, passwordValidation.map { it.message })
    }
    return Pair(user, listOf())
}

fun validateRegistrationAttempt(username: String?, password: String?): Pair<UserDTO?, List<String>> {
    val usernameValidation = validateUsername(username, false)
    val usernameErrors = usernameValidation.first
    val user = usernameValidation.second
    if (usernameErrors.isNotEmpty()) {
        return Pair(null, usernameErrors.map { it.message }) // don't bother with password validation
    }

    val passwordValidation = validatePassword(user, password)
    if (passwordValidation.isNotEmpty()) {
        return Pair(null, passwordValidation.map { it.message })
    }
    return Pair(user, listOf())
}