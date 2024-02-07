package de.sam.base.user

import de.sam.base.utils.KValidator
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
        val validator = KValidator(username)
            .check({ it.isNotBlank() }, "Username is required")
            .check({ it.length <= 20 }, "Username is too long")
            .check({ it.length >= 3 }, "Username is too short")

        return Pair(validator.isValid(), validator.errors())
    }

    fun validatePassword(password: String?): Pair<Boolean, List<String>> {
        val validator = KValidator(password)
            .check({ it.isNotBlank() }, "Password is required")
            .check({ it.length <= 128 }, "Password is too long")
            .check({ it.length >= 3 }, "Password is too short")

        return Pair(validator.isValid(), validator.errors())
    }

    fun validateRoles(roles: String): Pair<Boolean, List<String>> {
        val validator = KValidator(roles)
            .check({ it.isNotBlank() }, "Roles must not be empty")
            .check({
                it.split(",")
                    .all { splitRole -> splitRole in UserRoles.entries.map { role -> role.name } }
            }, "Invalid roles")

        return Pair(validator.isValid(), validator.errors())
    }
}
