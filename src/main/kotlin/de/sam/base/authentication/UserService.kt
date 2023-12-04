package de.sam.base.authentication

import de.sam.base.controllers.FileController
import de.sam.base.database.FileDTO
import de.sam.base.database.UserDTO
import de.sam.base.database.jdbi
import de.sam.base.services.FileService
import de.sam.base.services.LoginLogService
import de.sam.base.users.UserRoles
import org.jdbi.v3.core.kotlin.mapTo
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.*
import kotlin.jvm.optionals.getOrNull

class UserService : KoinComponent {
    private val loginLogService: LoginLogService by inject()
    private val fileService: FileService by inject()

    /**
     * Retrieves a user from the database based on the username.
     *
     * @param username the username of the user to retrieve
     * @return the UserDTO object representing the user, or null if the user does not exist or an error occurred
     */
    fun getUserByUsername(username: String?): UserDTO? {
        val sql = """
            SELECT * FROM t_users
            WHERE name ILIKE :name;
        """.trimIndent()

        try {
            return jdbi.withHandle<UserDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("name", username)
                    .mapTo<UserDTO>()
                    .findOne()
                    .getOrNull()
            }
        } catch (e: Exception) {
            Logger.error(e)
        }
        return null
    }

    /**
     * Creates a new user with the given parameters.
     *
     * @param username The username of the user.
     * @param passwordHash The password hash of the user.
     * @param passwordSalt The password salt of the user.
     * @param role The role of the user (default is UserRoles.USER).
     * @return The created UserDTO object if successful, null otherwise.
     *//*
    * CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    * ALTER TABLE public.t_users ALTER COLUMN id SET default uuid_generate_v4();
    */
    fun createUser(
        username: String,
        passwordHash: String,
        passwordSalt: String,
        role: UserRoles = UserRoles.USER
    ): UserDTO? {
        var userDTO: UserDTO? = null

        val roleId = role.name
        val preferences = ""
        val registrationDate = DateTime.now().toDate()

        val sqlInsert = """
            INSERT INTO t_users (name, password, roles, preferences, registration_date, salt)
            VALUES (:name, :password, :roles, :preferences, :registration_date, :salt);
        """.trimIndent()

        val sqlUpdateRootFolderId = """
            UPDATE t_users
            SET root_folder_id = :root_folder_id
            WHERE id = :id;
        """.trimIndent()

        try {
            jdbi.useTransaction<Exception> { handle ->
                handle.createUpdate(sqlInsert)
                    .bind("name", username)
                    .bind("password", passwordHash)
                    .bind("roles", roleId)
                    .bind("preferences", preferences)
                    .bind("registration_date", registrationDate)
                    .bind("salt", passwordSalt)
                    .execute()

                userDTO = getUserByUsername(username) // handle,
                    ?: throw Exception("Failed to create user")

                val rootFolder = FileDTO(
                    UUID.randomUUID(),
                    "My Files",
                    "/",
                    "null",
                    null,
                    userDTO!!.id,
                    0,
                    "0 B",
                    null,
                    DateTime.now(),
                    true,
                    null,
                    true
                )

                // create root folder
                fileService.createFile(
                    handle,
                    rootFolder,
                )

                handle.createUpdate(sqlUpdateRootFolderId)
                    .bind("root_folder_id", rootFolder.id)
                    .bind("id", userDTO!!.id)
                    .execute()

                userDTO!!.rootFolderId = rootFolder.id
            }

        } catch (e: Exception) {
            Logger.error(e)
        }
        return userDTO
    }


    /**
     * Deletes user data for the specified user.
     *
     * @param userId The ID of the user to delete.
     * @throws Exception if any error occurs during the deletion process.
     */
    fun deleteUser(userId: UUID) {
//        shareService.deleteAllSharesForUser(userId)
        Logger.debug("Deleting user data for $userId")

        Logger.debug("Deleting shares for user $userId")
        loginLogService.deleteAllLoginLogsForUser(userId)

        Logger.debug("Deleting root folder and all files for user $userId")
        val rootFolder = fileService.getRootFolderForUser(userId)
            ?: throw Exception("Failed to get root folder for user $userId")

        FileController().deleteFileList(listOf(rootFolder.id), userId)

        Logger.debug("Deleting orphaned files for user $userId")
        fileService.deleteAllFilesFromUser(userId)

        val sql = """
            DELETE FROM t_users
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        Logger.debug("Deleting user $userId")
        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", userId.toString())
                    .execute()
            }
        } catch (e: Exception) {
            throw Exception("Could not delete user $userId", e)
        }

    }

    /**
     * Returns the total number of users in the database.
     *
     * @return the total number of users
     */
    fun countTotalUsers(): Int {
        val sql = """
            SELECT COUNT(*) FROM t_users;
        """.trimIndent()

        return try {
            jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery(sql)
                    .mapTo<Int>()
                    .one()
            }
        } catch (e: Exception) {
            Logger.error(e)
            0
        }
    }
}