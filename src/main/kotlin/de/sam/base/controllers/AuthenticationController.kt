package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.User
import de.sam.base.database.UserDAO
import de.sam.base.database.toUser
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.prolongAtLeast
import io.javalin.core.validation.ValidationError
import io.javalin.http.Context
import io.javalin.http.HttpCode
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.measureTimeMillis

class AuthenticationController {
    private val argon2Instance = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)

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
                ctx.status(HttpCode.FORBIDDEN)
                ctx.json(attempt.second)
                return@prolongAtLeast
            }

            ctx.status(HttpCode.OK)
            ctx.currentUser = attempt.first
        }
    }

    fun registrationRequest(ctx: Context) {
        if (ctx.isLoggedIn && ctx.currentUser!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            ctx.json("You are already registered.")
            ctx.status(HttpCode.FORBIDDEN)
        } else if (!ctx.isLoggedIn && !config.allowUserRegistration) {
            ctx.json("User registration is currently disabled.")
            ctx.status(HttpCode.FORBIDDEN)
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
                    ctx.status(HttpCode.FORBIDDEN)
                    ctx.json(errors.map { it.message })
                } else {
                    val userDAO = transaction {
                        addLogger(StdOutSqlLogger)
                        return@transaction UserDAO.new {
                            this.name = username!!
                            this.password = Password.hash(password)
                                .addSalt("${this.id}") // argon2id salts the passwords on itself, but better safe than sorry
                                .addPepper(config.passwordPepper)
                                .with(argon2Instance)
                                .result
                            this.roles = UserRoles.USER.name
                            this.hidden = false
                            this.preferences = "{}"
                            this.registrationDate = DateTime.now()
                        }
                    }
                    ctx.currentUser = userDAO.toUser()
                    ctx.status(200)
                }
            }
        }
    }

    fun logoutRequest(ctx: Context) {
        ctx.req.session.invalidate()
    }
}

fun validateLoginAttempt(username: String?, password: String?): Pair<User?, List<String>> {
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