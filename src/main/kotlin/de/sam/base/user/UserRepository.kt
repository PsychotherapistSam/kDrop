package de.sam.base.user

import de.sam.base.database.UserDTO
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.joda.time.DateTime
import org.tinylog.kotlin.Logger
import java.sql.SQLException
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
    fun deleteAllSessions(): Boolean
    fun updateLastLoginTime(userId: UUID, dateTime: DateTime): Boolean
    fun countTotalUsers(): Int?

    fun <T> executeWithExceptionHandling(messageType: String? = null, block: () -> T): T? {
        return try {
            block()
        } catch (e: UnableToExecuteStatementException) {
            Logger.error("Unable to execute statement", e)
            null
        } catch (e: SQLException) {
            Logger.error("Database error ${messageType?.let { "with $it" }}", e)
            null
        } catch (e: Exception) {
            Logger.error("Unexpected error ${messageType?.let { "with $it" }}", e)
            null
        }
    }

}
