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

class UserService : KoinComponent {
    private val loginLogService: LoginLogService by inject()
    private val fileService: FileService by inject()

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
                    .orElse(null)
            }
        } catch (e: Exception) {
            Logger.error(e)
        }
        return null
    }

    /*
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

        /**
         * transaction {
         *             // delete all user related data
         *             ShareDAO.find { SharesTable.user eq user.id }.forEach { it.delete() }
         *
         *             // delete login log
         *             LoginLogDAO.find { LoginLogTable.user eq user.id }.forEach { it.delete() }
         *
         *             // delete all files
         *             FileController(fileService).deleteFileList(
         *                 FileDAO.find { FilesTable.owner eq user.id and FilesTable.isRoot }.map { it.id.value }.take(1),
         *                 user
         *             )
         *
         *             // delete all files
         *             FileDAO.find { FilesTable.owner eq user.id }.forEach { it.delete() }
         *
         *             UserDAO
         *                 .findById(user.id)!!
         *                 .delete()
         *  }
         */


//        val sql = """
//            SELECT * FROM t_files
//            WHERE id = CAST(:id AS uuid);
//        """.trimIndent()
//
//        try {
//            jdbi.withHandle<FileDTO?, Exception> { handle ->
//                handle.createQuery(sql)
//                    .bind("id", fileID.toString())
//                    .mapTo<FileDTO>()
//                    .findOne()
//                    .orElse(null)
//            }
//        } catch (e: Exception) {
//        }
    }
}