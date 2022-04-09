package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.database.DatabaseManager.User
import de.sam.base.database.DatabaseManager.UsersTable
import de.sam.base.site.configuration.Configuration
import de.sam.base.utils.getUser
import io.javalin.http.Context
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class AuthenticationController {

    private val argon2Instance = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)

    fun loginRequest(ctx: Context) {
        if (ctx.getUser() != null) {
            ctx.status(200)
            return
        }

        val start = System.currentTimeMillis()

        val usernameValidator = ctx.formParamAsClass<String>("username")
            .check({ it.isNotBlank() }, "Username is required")
            .check({ it.length <= 20 }, "Username is too long")
            .check({ it.length >= 3 }, "Username is too short")

        val passwordValidator = ctx.formParamAsClass<String>("password")
            .check({ it.isNotBlank() }, "Password is required")
            .check({ it.length <= 128 }, "Password is too long")
            .check({ it.length >= 3 }, "Password is too short")

        val errors = usernameValidator.errors() + passwordValidator.errors()
        if (errors.isNotEmpty()) {
            ctx.status(400)
            ctx.json(errors)
            return
        }

        val username = usernameValidator.get()
        val password = passwordValidator.get()

        val user = transaction {
            addLogger(StdOutSqlLogger)
            return@transaction User
                .find { UsersTable.name.lowerCase() eq username.lowercase() }
                .limit(1)
                .firstOrNull()
        }



        if (user != null) {
            val passwordIsVerified = Password.check(password, user.password)
                .addSalt("${user.id}${user.name}") // argon2id salts the passwords on itself, but better safe than sorry
                .addPepper(Configuration.passwordPepper)
                .with(argon2Instance)

            if (passwordIsVerified) {
                ctx.sessionAttribute("user", user)
                ctx.status(200)
            } else {
                ctx.status(401)
            }
        }

        // each request is going to take at least 2s to avoid guessing if a user exists -> checking if the password is actually getting hashed

        val end = System.currentTimeMillis()

        val waitTime = 1900L + Random().nextInt(200)

        val diffTime: Long = waitTime - (end - start)
        if (diffTime > 0) Thread.sleep(diffTime)
    }

    fun registrationRequest(ctx: Context) {
        val start = System.currentTimeMillis()

        val usernameValidator = ctx.formParamAsClass<String>("username")
            .check({ it.isNotBlank() }, "Username is required")
            .check({ it.length <= 20 }, "Username is too long")
            .check({ it.length >= 3 }, "Username is too short")
            .check(
                {
                    val transaction = transaction {
                        addLogger(StdOutSqlLogger)
                        !User.find { UsersTable.name.lowerCase() like it.lowercase() }.any()
                    }
                    transaction
                },
                "Username is already taken"
            ) // kinda scuffed but I'll take it - it stays like this for now

        val passwordValidator = ctx.formParamAsClass<String>("password")
            .check({ it.isNotBlank() }, "Password is required")
            .check({ it.length <= 128 }, "Password is too long")
            .check({ it.length >= 3 }, "Password is too short")


        val errors = usernameValidator.errors() + passwordValidator.errors()
        if (errors.isNotEmpty()) {
            ctx.status(400)
            ctx.json(errors)
            return
        }

        val username = usernameValidator.get()
        val password = passwordValidator.get()

        val user = transaction {
            addLogger(StdOutSqlLogger)
            return@transaction User.new {
                this.name = username
                this.password = Password.hash(password)
                    .addSalt("${this.id}${this.name}") // argon2id salts the passwords on itself, but better safe than sorry
                    .addPepper(Configuration.passwordPepper)
                    .with(argon2Instance)
                    .result
                this.roles = "0"
                this.hidden = false
                this.preferences = "{}"
                this.registrationDate = DateTime.now()
            }
        }

        // each request is going to take at least 2s to avoid guessing if a user exists -> checking if the password is actually getting hashed

        val end = System.currentTimeMillis()

        val waitTime = 1900L + Random().nextInt(200)

        val diffTime: Long = waitTime - (end - start)
        if (diffTime > 0) Thread.sleep(diffTime)

        ctx.sessionAttribute("user", user)
        ctx.status(200)
        return
    }
}