package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import de.sam.base.database.toUser
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import de.sam.base.utils.isLoggedIn
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
        if (ctx.isLoggedIn) {
            ctx.status(200)
            return
        }

        // hide discrepancy on whether an account exists or not to prevent account enumeration
        prolongAtLeast(2000) {
            val usernameValidation = validateUsername(ctx.formParam("username"))
            val usernameErrors = usernameValidation.first
            val user = usernameValidation.second
            if (usernameErrors.isNotEmpty()) {
                ctx.status(HttpCode.FORBIDDEN)
                ctx.json(usernameErrors.map { it.message })
            } else {
                val passwordValidation = validatePassword(user, ctx.formParam("password"))
                if (passwordValidation.isNotEmpty()) {
                    ctx.status(HttpCode.FORBIDDEN)
                    ctx.json(passwordValidation.map { it.message })
                } else {
                    ctx.currentUser = user!!
                    ctx.status(200)
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun prolongAtLeast(ms: Long, randomTime: Long = 200, block: () -> Unit): Unit? {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        var test: Unit? = null
        val executionTime = measureTimeMillis {
            test = block()
        }

        val waitTime = ms - (randomTime / 2) + Random().nextInt(200)

        val timeDiff: Long = waitTime - executionTime
        if (timeDiff > 0) Thread.sleep(timeDiff)
        return test
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
                                .addSalt("${this.id}${this.name}") // argon2id salts the passwords on itself, but better safe than sorry
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