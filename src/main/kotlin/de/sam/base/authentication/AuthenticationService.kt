package de.sam.base.authentication

import de.sam.base.user.UserRepository
import de.sam.base.user.UserRoles
import de.sam.base.user.UserValidator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


class AuthenticationService : KoinComponent {
    private val userRepository: UserRepository by inject()

    private val userValidator: UserValidator by inject()
    private val passwordHasher: PasswordHasher by inject()

    fun login(username: String, password: String): AuthenticationResult {
        val (isValid, errors) = userValidator.validateCredentials(username, password)
        if (!isValid) {
            return AuthenticationResult.Failure(errors)
        }

        val userDTO = userRepository.getUserByUsername(username)
            ?: return AuthenticationResult.Failure(listOf("Invalid username or password"))

        var salt = userDTO.salt

        if (salt.isNullOrEmpty()) {
            salt = userDTO.id.toString()
        }

        if (!passwordHasher.verifyPassword(password, userDTO.password, salt)) {
            return AuthenticationResult.Failure(listOf("Invalid username or password"))
        }

        if (userDTO.totpSecret != null) {
            return AuthenticationResult.Success(userDTO, true)
        }

        return AuthenticationResult.Success(userDTO)
    }

    fun register(username: String, password: String): AuthenticationResult {
        if (userRepository.getUserByUsername(username) != null) {
            return AuthenticationResult.Failure(listOf("Username already taken"))
        }

        val salt = UUID.randomUUID().toString()
        val hashedPassword = passwordHasher.hashPassword(password, salt)

        val role = if (userRepository.countTotalUsers() == 0)
            UserRoles.ADMIN
        else
            UserRoles.USER

        val newUser = userRepository.createUser(username, hashedPassword, salt, role)
            ?: return AuthenticationResult.Failure(listOf("Failed to create user"))

        return AuthenticationResult.Success(newUser)
    }
}