package de.sam.base.authentication

import de.sam.base.users.UserRoles
import io.javalin.validation.Validator
import org.koin.core.component.KoinComponent

class UserValidator : KoinComponent {

    fun validateCredentials(username: String?, password: String?): Pair<Boolean, List<String>> {
        val (usernameValid, usernameErrors) = validateUsername(username)
        val (passwordValid, passwordErrors) = validatePassword(password)

        val errors = mutableListOf<String>()
        if (!usernameValid) errors.addAll(usernameErrors)
        if (!passwordValid) errors.addAll(passwordErrors)

        return Pair(errors.isEmpty(), errors)
    }

    fun validateUsername(username: String?): Pair<Boolean, List<String>> {
        val fieldName = "username"

        val validator = Validator.create(String::class.java, username, fieldName)
            .check({ it.isNotBlank() }, "Username is required")
            .check({ it.length <= 20 }, "Username is too long")
            .check({ it.length >= 3 }, "Username is too short")

        val errors = validator.errors()[fieldName]?.map { it.message }.orEmpty()
        return Pair(errors.isEmpty(), errors)
    }

    fun validatePassword(password: String?): Pair<Boolean, List<String>> {
        val fieldName = "password"

        val validator = Validator.create(String::class.java, password, fieldName)
            .check({ it.isNotBlank() }, "Password is required")
            .check({ it.length <= 128 }, "Password is too long")
            .check({ it.length >= 3 }, "Password is too short")

        val errors = validator.errors()[fieldName]?.map { it.message }.orEmpty()
        return Pair(errors.isEmpty(), errors)
    }

    fun validateRoles(roles: String): Pair<Boolean, List<String>> {
        val fieldName = "role"

        val validator = Validator.create(String::class.java, roles, fieldName)
            .check({ it.isNotBlank() }, "Roles must not be empty")
            .check({
                it.split(",")
                    .all { splitRole -> splitRole in UserRoles.values().map { role -> role.name } }
            }, "Invalid roles")
        val errors = validator.errors()[fieldName]?.map { it.message }.orEmpty()
        return Pair(errors.isEmpty(), errors)

    }
}
