package de.sam.base.authentication

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


class AuthenticationService : KoinComponent {
    private val userService: UserService by inject()

    private val userValidator: UserValidator by inject()
    private val passwordHasher: PasswordHasher by inject()

    fun login(username: String, password: String): AuthenticationResult {
        val (isValid, errors) = userValidator.validateCredentials(username, password)
        if (!isValid) {
            return AuthenticationResult.Failure(errors)
        }

        val userDTO = userService.getUserByUsername(username)
            ?: return AuthenticationResult.Failure(listOf("Invalid username or password"))

        var salt = userDTO.salt

        if (salt.isNullOrEmpty()) {
            salt = userDTO.id.toString()
        }

        if (!passwordHasher.verifyPassword(password, userDTO.password, salt)) {
            return AuthenticationResult.Failure(listOf("Invalid username or password"))
        }

        return AuthenticationResult.Success(userDTO)
    }

    fun register(username: String, password: String): AuthenticationResult {
        if (userService.getUserByUsername(username) != null) {
            return AuthenticationResult.Failure(listOf("Username already taken"))
        }

        val salt = UUID.randomUUID().toString()
        val hashedPassword = passwordHasher.hashPassword(password, salt)

        val newUser = userService.createUser(username, hashedPassword, salt)
            ?: return AuthenticationResult.Failure(listOf("Failed to create user"))

        return AuthenticationResult.Success(newUser)
    }
}