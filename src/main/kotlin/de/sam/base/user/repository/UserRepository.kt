package de.sam.base.user.repository

import de.sam.base.database.UserDTO
import de.sam.base.user.UserRoles
import org.joda.time.DateTime
import java.util.*

interface UserRepository {
    fun getUserByUsername(username: String?): UserDTO?

    fun createUser(
        username: String,
        passwordHash: String,
        passwordSalt: String,
        role: UserRoles = UserRoles.USER
    ): UserDTO?

    fun deleteUser(userId: UUID): Boolean
    fun updateUser(copy: UserDTO): UserDTO
    fun getUserById(it: UUID): UserDTO?
    fun searchUsers(searchQuery: String, limit: Int = 25, offset: Int = 0): List<UserDTO>
    fun getAllUsers(limit: Int = 25, offset: Int = 0): List<UserDTO>
    fun deleteAllSessions()
    fun updateLastLoginTime(userId: UUID, dateTime: DateTime)
    fun countTotalUsers(): Int

}
