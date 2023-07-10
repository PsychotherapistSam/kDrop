package de.sam.base.services

import de.sam.base.database.FileDTO
import de.sam.base.database.jdbi
import de.sam.base.exceptions.FileServiceException
import de.sam.base.utils.humanReadableByteCountBin
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.joda.time.DateTime
import org.tinylog.kotlin.Logger
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class FileService {

    /**
     * Fetches a file from the database by its ID.
     *
     * @param fileID the UUID of the file to fetch.
     * @return the file or null if it does not exist.
     * @throws FileServiceException if the file could not be fetched.
     */
    fun getFileById(fileID: UUID): FileDTO? {
        val sql = """
            SELECT * FROM t_files 
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", fileID.toString())
                    .mapTo<FileDTO>()
                    .findOne()
                    .orElse(null)
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch file with id $fileID", e)
        }
    }

    /**
     * Generates a list of files that represent the breadcrumb of a file.
     * The list is ordered from the root folder to the file.
     *
     * @param fileID the UUID of the file to generate the breadcrumb for.
     * @return the list of files that represent the breadcrumb.
     * @throws FileServiceException if the breadcrumb could not be generated.
     */
    fun getFileBreadcrumb(fileID: UUID): List<FileDTO> {
        val sql = """
        WITH RECURSIVE breadcrumb AS (
            SELECT *, 1 as depth
            FROM t_files
            WHERE id = CAST(:id AS uuid)

            UNION ALL

            SELECT t.*, b.depth + 1
            FROM t_files t
            JOIN breadcrumb b ON b.parent = t.id
        )
        SELECT * FROM breadcrumb ORDER BY depth DESC;
    """.trimIndent()

        return try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", fileID.toString())
                    .mapTo<FileDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch breadcrumb for file with id $fileID", e)
        }
    }

    /**
     * Retrieves the contents of a folder for a specific user.
     *
     * @param userId The ID of the user.
     * @param folderId The ID of the folder.
     * @return A list of FileDTO objects representing the contents of the folder.
     * @throws FileServiceException If there is an error fetching the folder contents.
     */
    fun getFolderContentForUser(userId: UUID, folderId: UUID): List<FileDTO> {
        val sql = """
            SELECT * FROM t_files WHERE owner = CAST(:userId AS uuid) 
            AND parent = CAST(:folderId AS uuid);
        """.trimIndent()

        return try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId)
                    .bind("folderId", folderId)
                    .mapTo<FileDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException(
                "Error fetching folder content for user with ID $userId and folder ID $folderId",
                e
            )
        }
    }

    /**
     * Retrieves the root folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return A FileDTO object representing the root folder, or null if no root folder was found.
     * @throws FileServiceException If there is an error fetching the root folder.
     */
    fun getRootFolderForUser(userId: UUID): FileDTO? {
        val sql = """
            SELECT * FROM t_files
            WHERE owner = CAST(:owner AS uuid) AND is_root = TRUE;
        """.trimIndent()

        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("owner", userId.toString())
                    .mapTo<FileDTO>()
                    .findOne()
                    .orElse(null)
            }
        } catch (e: Exception) {
            throw FileServiceException("Error fetching root folder for user with ID $userId", e)
        }
    }

    /**
     * Recalculates the size of a folder and updates it in the database.
     *
     * @param folderId The ID of the folder.
     * @param userId The ID of the user.
     * @throws FileServiceException If there is an error recalculating the folder size.
     */
    @OptIn(ExperimentalTime::class)
    fun recalculateFolderSize(folderId: UUID, userId: UUID) {
        try {
            measureTime {
                var folder: FileDTO? = getFileById(folderId)

                while (folder != null) {
                    Logger.debug("updating folder ${folder.name} which was ${folder.sizeHR}")
                    val size = calculateFolderSize(folder.id)
                    folder = updateFolderSize(folder.id, size, DateTime.now())
                    Logger.debug("folder is now ${folder?.sizeHR}")
                    folder = folder?.parent?.let { getFileById(it) }
                }
            }.let { Logger.debug("refreshed filesize tree in ${it.toLong(DurationUnit.MILLISECONDS)}ms") }
        } catch (e: Exception) {
            throw FileServiceException("Error recalculating folder size for folder with ID $folderId", e)
        }
    }

    private fun calculateFolderSize(folderId: UUID): Long {
        val sql = """
        WITH RECURSIVE file_tree AS (
            SELECT * FROM t_files WHERE parent = CAST(:id AS uuid)
            UNION ALL
            SELECT t_files.* FROM t_files, file_tree WHERE t_files.parent = file_tree.id
        )
        SELECT SUM(size) FROM file_tree
    """.trimIndent()

        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createQuery(sql)
                .bind("id", folderId.toString())
                .mapTo(Long::class.java)
                .findOne()
                .orElse(0)
        }
    }

    private fun updateFolderSize(folderId: UUID, size: Long, created: DateTime): FileDTO? {
        val sql = """
            UPDATE t_files
            SET size = :size, size_hr = :size_hr, created = :created
            WHERE id = CAST(:id AS uuid)
            RETURNING *;
        """.trimIndent()

        return jdbi.withHandle<FileDTO?, Exception> { handle ->
            handle.createUpdate(sql)
                .bind("id", folderId.toString())
                .bind("size", size)
                .bind("size_hr", humanReadableByteCountBin(size))
                .bind("created", created.toDate())
                .executeAndReturnGeneratedKeys()
                .mapTo<FileDTO>()
                .findOne()
                .orElse(null)
        }
    }

    fun createFile(handle: Handle, file: FileDTO): FileDTO {
        val sql = """
        INSERT INTO t_files (id, name, path, mime_type, parent, owner, size, size_hr, password, private, created, is_folder, is_root)
        VALUES (CAST(:id AS uuid), :name, :path, :mime_type, CAST(:parent AS uuid), CAST(:owner AS uuid), :size, :size_hr, :password, :private, :created, :is_folder, :is_root)
        RETURNING *;
    """.trimIndent()

        return try {
            handle.createUpdate(sql)
                .bind("id", file.id.toString())
                .bind("name", file.name)
                .bind("path", file.path)
                .bind("mime_type", file.mimeType)
                .bind("parent", file.parent?.toString())
                .bind("owner", file.owner.toString())
                .bind("size", file.size)
                .bind("size_hr", file.sizeHR)
                .bind("password", file.password)
                .bind("private", file.private)
                .bind("created", file.created?.toDate())
                .bind("is_folder", file.isFolder)
                .bind("is_root", file.isRoot)
                .executeAndReturnGeneratedKeys()
                .mapTo<FileDTO>()
                .findOne()
                .orElse(null)
        } catch (e: Exception) {
            throw FileServiceException("Error executing createFile")
        }
    }

    /**
     * Updates a file in the database.
     *
     * @param file the file to update.
     * @return the updated file or null if it does not exist.
     * @throws FileServiceException if the file could not be updated.
     */
    fun updateFile(file: FileDTO): FileDTO? {
        val sql = """
            UPDATE t_files
            SET name = :name, path = :path, mime_type = :mime_type, parent = CAST(:parent AS uuid), 
                owner = CAST(:owner AS uuid), size = :size, size_hr = :size_hr, password = :password, 
                private = :private, created = :created, is_folder = :is_folder, is_root = :is_root
            WHERE id = CAST(:id AS uuid)
            RETURNING *;
        """.trimIndent()

        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", file.id.toString())
                    .bind("name", file.name)
                    .bind("path", file.path)
                    .bind("mime_type", file.mimeType)
                    .bind("parent", file.parent?.toString())
                    .bind("owner", file.owner.toString())
                    .bind("size", file.size)
                    .bind("size_hr", file.sizeHR)
                    .bind("password", file.password)
                    .bind("private", file.private)
                    .bind("created", file.created?.toDate())
                    .bind("is_folder", file.isFolder)
                    .bind("is_root", file.isRoot)
                    .executeAndReturnGeneratedKeys()
                    .mapTo<FileDTO>()
                    .findOne()
                    .orElse(null)
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not update file with id ${file.id}", e)
        }
    }

    /**
     * Fetches a list of files from the database by their IDs.
     *
     * @param fileIDs the list of UUIDs of the files to fetch.
     * @return a list of files or an empty list if no files match the provided IDs.
     * @throws FileServiceException if the files could not be fetched.
     */
    fun getFilesByIds(fileIDs: List<UUID>): List<FileDTO> {
        val placeholders = fileIDs.mapIndexed { index, _ -> ":id$index" }
        val sql = """
            SELECT * FROM t_files 
            WHERE id IN (${placeholders.joinToString()})
        """.trimIndent()

        return try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                val query = handle.createQuery(sql)

                // Bind each ID to its corresponding placeholder
                for ((index, id) in fileIDs.withIndex()) {
                    query.bind("id$index", id)
                }

                query.mapTo<FileDTO>().list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch files with ids $fileIDs", e)
        }
    }

    //TODO: return this to without jdbc
    /**
     * Fetches a list of all files and folders by their parent ID.
     *
     * @param fileIDs the list of UUIDs of the files to fetch.
     * @return a list of files or an empty list.
     * @throws FileServiceException if the files could not be fetched.
     */
    fun getAllFilesFromFolderListRecursively(fileIDs: List<UUID>): List<FileDTO> {
        val sql = """
            WITH RECURSIVE file_tree AS (
                SELECT *
                FROM t_files
                WHERE id = ANY(?)
    
                UNION ALL
    
                SELECT f.*
                FROM t_files f
                JOIN file_tree ft ON f.parent = ft.id
            )
            SELECT * FROM file_tree
        """.trimIndent()
        return jdbi.withHandle<List<FileDTO>, Exception> { handle ->
            val conn = handle.jdbi.open().connection
            val uuidArray = conn.createArrayOf("uuid", fileIDs.map { it.toString() }.toTypedArray())

            handle.createQuery(sql)
                .bind(0, uuidArray)
                .mapTo<FileDTO>().list()
        }
    }

    /**
     * Deletes a list of files and any associated shares from the database.
     *
     * @param fileIDs the list of UUIDs of the files to delete.
     * @return a list of files that were deleted.
     * @throws FileServiceException if the files and shares could not be deleted.
     */
    fun deleteFilesAndShares(fileIDs: List<UUID>): List<FileDTO> {
        val fileSql = """
            DELETE FROM t_files
            WHERE id = ANY(CAST(:ids AS uuid[]))
            RETURNING *;
        """.trimIndent()

        val sharesSql = """
            DELETE FROM t_shares
            WHERE file = ANY(CAST(:ids AS uuid[]));
        """.trimIndent()

        return try {
            val deletedFiles: MutableList<FileDTO> = mutableListOf()

            jdbi.useTransaction<Exception> { handle ->
                val conn = handle.jdbi.open().connection
                val uuidArray = conn.createArrayOf("uuid", fileIDs.map { it.toString() }.toTypedArray())

                // Delete shares
                handle.createUpdate(sharesSql)
                    .bind("ids", uuidArray)
                    .execute()

                // Delete files and get them
                handle.createQuery(fileSql)
                    .bind("ids", uuidArray)
                    .mapTo<FileDTO>()
                    .list()
                    .let { deletedFiles.addAll(it) }
            }

            deletedFiles
        } catch (e: Exception) {
            throw FileServiceException("Could not delete files and shares with ids $fileIDs", e)
        }
    }

}