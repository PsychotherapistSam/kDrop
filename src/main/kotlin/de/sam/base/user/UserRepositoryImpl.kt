package de.sam.base.user

import de.sam.base.authentication.log.LoginLogRepository
import de.sam.base.database.FileDTO
import de.sam.base.database.UserDTO
import de.sam.base.database.jdbi
import de.sam.base.file.FileController
import de.sam.base.file.repository.FileRepository
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.sql.SQLException
import java.util.*
import kotlin.jvm.optionals.getOrNull

class UserRepositoryImpl : UserRepository, KoinComponent {
    private val loginLogRepository: LoginLogRepository by inject()
    private val fileRepository: FileRepository by inject()

    /**
     * Retrieves a user from the database based on the username.
     *
     * @param username the username of the user to retrieve
     * @return the UserDTO object representing the user, or null if the user does not exist or an error occurred
     */
    override fun getUserByUsername(username: String?): UserDTO? {
        val sql = """
            SELECT * FROM t_users
            WHERE name ILIKE :name;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<UserDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("name", username)
                    .mapTo<UserDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
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
    override fun createUser(username: String, passwordHash: String, passwordSalt: String, role: UserRoles): UserDTO? {
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
                try {
                    handle.createUpdate(sqlInsert)
                        .bind("name", username)
                        .bind("password", passwordHash)
                        .bind("roles", roleId)
                        .bind("preferences", preferences)
                        .bind("registration_date", registrationDate)
                        .bind("salt", passwordSalt)
                        .execute()
                } catch (e: Exception) {
                    Logger.error("Unable to execute insert query: $sqlInsert", e)
                    throw e
                }

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
                fileRepository.createFile(
                    handle,
                    rootFolder,
                )

                try {
                    handle.createUpdate(sqlUpdateRootFolderId)
                        .bind("root_folder_id", rootFolder.id)
                        .bind("id", userDTO!!.id)
                        .execute()
                } catch (e: Exception) {
                    Logger.error("Unable to execute update query: $sqlUpdateRootFolderId", e)
                    throw e
                }

                // updating the returned userDTO with the root folder ID for consistency
                userDTO!!.rootFolderId = rootFolder.id
            }
            return userDTO
        } catch (e: UnableToExecuteStatementException) {
            Logger.error("Unable to execute statement", e)
        } catch (e: SQLException) {
            Logger.error("Database error", e)
        } catch (e: Exception) {
            Logger.error("Unexpected error", e)
        }
        return null
    }


    /**
     * Deletes user data for the specified user.
     *
     * @param userId The ID of the user to delete.
     * @throws Exception if any error occurs during the deletion process.
     */
    override fun deleteUser(userId: UUID): Boolean {
//        shareService.deleteAllSharesForUser(userId)
        Logger.debug("Deleting user data for $userId")

        Logger.debug("Deleting shares for user $userId")
        loginLogRepository.deleteAllLoginLogsForUser(userId)

        Logger.debug("Deleting root folder and all files for user $userId")
        val rootFolder = fileRepository.getRootFolderForUser(userId)
            ?: throw Exception("Failed to get root folder for user $userId")

        FileController().deleteFileList(listOf(rootFolder.id), userId)

        Logger.debug("Deleting orphaned files for user $userId")
        fileRepository.deleteAllFilesFromUser(userId)

        val sql = """
            DELETE FROM t_users
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        Logger.debug("Deleting user $userId")
        return executeWithExceptionHandling {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", userId.toString())
                    .execute()
            }
            true
        } != null
    }

    /**
     * Returns the total number of users in the database.
     *
     * @return the total number of users
     */
    override fun countTotalUsers(): Int? {
        val sql = """
            SELECT COUNT(*) FROM t_users;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery(sql)
                    .mapTo<Int>()
                    .one()
            }
        }
    }

    /**
     * Updates the user with the given data.
     *
     * @param copy the UserDTO object containing the updated data
     * @return the updated UserDTO object
     */
    override fun updateUser(copy: UserDTO): UserDTO {
        val sql = """
            UPDATE t_users
            SET name = :name,
                password = :password,
                roles = :roles,
                preferences = :preferences,
                registration_date = :registration_date,
                totp_secret = :totp_secret,
                root_folder_id = :root_folder_id,
                salt = :salt
            WHERE id = :id;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("name", copy.name)
                    .bind("password", copy.password)
                    .bind("roles", copy.roles.joinToString(","))
                    .bind("preferences", copy.preferences)
                    .bind("registration_date", copy.registrationDate.toDate())
                    .bind("totp_secret", copy.totpSecret)
                    .bind("root_folder_id", copy.rootFolderId)
                    .bind("salt", copy.salt)
                    .bind("id", copy.id)
                    .execute()
            }
            copy
        }!!
    }

    /**
     * Retrieves a user from the database based on the user ID.
     *
     * @param it the user ID
     * @return the UserDTO object representing the user, or null if the user does not exist or an error occurred
     */
    override fun getUserById(it: UUID): UserDTO? {
        val sql = """
            SELECT * FROM t_users
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<UserDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", it.toString())
                    .mapTo<UserDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    /**
     * Searches for users with the given search query.
     *
     * @param searchQuery the search query
     * @return a list of UserDTO objects representing the users
     * @throws Exception if any error occurs during the search process
     */
    override fun searchUsers(searchQuery: String, limit: Int, offset: Int): List<UserDTO> {
        val sql = """
            SELECT * FROM t_users
            WHERE name ILIKE :name
            ORDER BY registration_date
            LIMIT :limit
            OFFSET :offset;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<List<UserDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("name", "%$searchQuery%")
                    .bind("limit", limit)
                    .bind("offset", offset)
                    .mapTo<UserDTO>()
                    .list()
            }
        } ?: emptyList()
    }

    /**
     * Retrieves a list of all users from the database.
     *
     * @param limit the maximum number of users to retrieve
     * @param offset the offset
     *
     */
    override fun getAllUsers(limit: Int, offset: Int): List<UserDTO> {
        val sql = """
            SELECT * FROM t_users
            ORDER BY registration_date
            LIMIT :limit
            OFFSET :offset;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<List<UserDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("limit", limit)
                    .bind("offset", offset)
                    .mapTo<UserDTO>()
                    .list()
            }
        } ?: emptyList()
    }

    /**
     * Deletes all sessions from the database.
     *
     * @throws Exception if any error occurs during the deletion process
     */
    override fun deleteAllSessions(): Boolean {
        val sql = """
            DELETE FROM jettysessions;
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .execute()
            }
            true
        } != null
    }

    /**
     * Updates the last login time for a user in the database.
     *
     * @param userId The ID of the user.
     * @param dateTime The last login time as a DateTime object.
     */
    override fun updateLastLoginTime(userId: UUID, dateTime: DateTime): Boolean {
        val sql = """
            UPDATE t_users
            SET last_login = :last_login
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("last_login", dateTime.toDate())
                    .bind("id", userId.toString())
                    .execute()
            }
            true
        } != null
    }
}