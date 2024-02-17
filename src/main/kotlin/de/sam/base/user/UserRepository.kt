package de.sam.base.user

import de.sam.base.database.SqlRepository
import de.sam.base.database.UserDTO
import org.joda.time.DateTime
import java.util.*

interface UserRepository : SqlRepository {
    /**
     * Retrieves a user from the database based on the username.
     *
     * @param username the username of the user to retrieve
     * @return the UserDTO object representing the user, or null if the user does not exist or an error occurred
     */
    fun getUserByUsername(username: String?): UserDTO?

    /**
     * Creates a new user with the given parameters.
     *
     * @param username The username of the user.
     * @param passwordHash The password hash of the user.
     * @param passwordSalt The password salt of the user.
     * @param role The role of the user (default is UserRoles.USER).
     * @return The created UserDTO object if successful, null otherwise.
     */
    fun createUser(
        username: String,
        passwordHash: String,
        passwordSalt: String,
        role: UserRoles = UserRoles.USER
    ): UserDTO?

    /**
     * Deletes user data for the specified user.
     *
     * @param userId The ID of the user to delete.
     * @throws Exception if any error occurs during the deletion process.
     */
    fun deleteUser(userId: UUID): Boolean

    /**
     * Returns the total number of users in the database.
     *
     * @return the total number of users
     */
    fun countTotalUsers(): Int?

    /**
     * Updates the user with the given data.
     *
     * @param copy the UserDTO object containing the updated data
     * @return the updated UserDTO object
     */
    fun updateUser(copy: UserDTO): UserDTO

    /**
     * Retrieves a user from the database based on the user ID.
     *
     * @param it the user ID
     * @return the UserDTO object representing the user, or null if the user does not exist or an error occurred
     */
    fun getUserById(it: UUID): UserDTO?

    /**
     * Searches for users with the given search query.
     *
     * @param searchQuery the search query
     * @return a list of UserDTO objects representing the users
     * @throws Exception if any error occurs during the search process
     */
    fun searchUsers(searchQuery: String, limit: Int = 25, offset: Int = 0): List<UserDTO>

    /**
     * Retrieves a list of all users from the database.
     *
     * @param limit the maximum number of users to retrieve
     * @param offset the offset
     *
     */
    fun getAllUsers(limit: Int = 25, offset: Int = 0): List<UserDTO>

    /**
     * Deletes all sessions from the database.
     *
     * @throws Exception if any error occurs during the deletion process
     */
    fun deleteAllSessions(): Boolean

    /**
     * Updates the last login time for a user in the database.
     *
     * @param userId The ID of the user.
     * @param dateTime The last login time as a DateTime object.
     */
    fun updateLastLoginTime(userId: UUID, dateTime: DateTime): Boolean

}
