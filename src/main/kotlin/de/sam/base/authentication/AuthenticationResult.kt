package de.sam.base.authentication

import de.sam.base.database.UserDTO

sealed class AuthenticationResult {


    data class Success(val userDTO: UserDTO) : AuthenticationResult()
    data class Failure(val errors: List<String>) : AuthenticationResult()
}