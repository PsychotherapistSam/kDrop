package de.sam.base.file

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import de.sam.base.database.FileDTO
import de.sam.base.database.jdbi
import de.sam.base.exceptions.FileServiceException
import de.sam.base.file.repository.FileRepository
import de.sam.base.file.sorting.FileSortingDirection
import de.sam.base.utils.file.humanReadableByteCountBin
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class FileRepositoryImpl : FileRepository, KoinComponent {
    override val fileCache: LoadingCache<UUID, FileDTO> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .expireAfterAccess(Duration.ofMinutes(5))
        .refreshAfterWrite(Duration.ofMinutes(5))
        .build(this::getFileById)

    /**
     * Fetches a file from the database by its ID.
     *
     * @param fileID the UUID of the file to fetch.
     * @return the file or null if it does not exist.
     * @throws FileServiceException if the file could not be fetched.
     */
    override fun getFileById(fileID: UUID): FileDTO? {
        val sql = """
            SELECT * FROM t_files 
            WHERE id = :id;
        """.trimIndent()

        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", fileID)
                    .mapTo<FileDTO>()
                    .findOne()
                    .getOrNull()
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
    override fun getFileBreadcrumb(fileID: UUID): List<FileDTO> {
        val sql = """
        WITH RECURSIVE breadcrumb AS (
            SELECT *, 1 as depth
            FROM t_files
            WHERE id = :id

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
                    .bind("id", fileID)
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
    override fun getFolderContentForUser(folderId: UUID, userId: UUID): List<FileDTO> {
        val sql = """
            SELECT * FROM t_files WHERE owner = :userId 
            AND parent = :folderId;
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
    override fun getRootFolderForUser(userId: UUID): FileDTO? {
        val sql = """
            SELECT * FROM t_files
            WHERE owner = :owner AND is_root = TRUE;
        """.trimIndent()

        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("owner", userId)
                    .mapTo<FileDTO>()
                    .one()
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
    override fun recalculateFolderSize(folderId: UUID, userId: UUID) {
        try {
            measureTime {
                var folder: FileDTO? = fileCache.get(folderId)

                while (folder != null) {
                    Logger.tag("Database").debug("updating folder ${folder.name} which was ${folder.sizeHR}")
                    val size = calculateFolderSize(folder.id)
                    folder = updateFolderSize(folder.id, size, DateTime.now())
                    Logger.tag("Database").debug("folder is now ${folder?.sizeHR}")
                    folder = folder?.parent?.let { getFileById(it) }
                }
            }.let {
                Logger.tag("Database").debug("refreshed filesize tree in ${it.toLong(DurationUnit.MILLISECONDS)}ms")
            }
        } catch (e: Exception) {
            throw FileServiceException("Error recalculating folder size for folder with ID $folderId", e)
        }
    }

    private fun calculateFolderSize(folderId: UUID): Long {
//        val sql = """
//        WITH RECURSIVE file_tree AS (
//            SELECT * FROM t_files WHERE parent = :id
//            UNION ALL
//            SELECT t_files.* FROM t_files, file_tree WHERE t_files.parent = file_tree.id
//        )
//        SELECT SUM(size) FROM file_tree
//    """.trimIndent()

        val sql = """
            SELECT SUM(size) FROM t_files WHERE parent = :id;
        """.trimIndent()

        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createQuery(sql)
                .bind("id", folderId)
                .mapTo(Long::class.java)
                .one()
                .or(0)
        }
    }

    private fun updateFolderSize(folderId: UUID, size: Long, created: DateTime): FileDTO? {
        val sql = """
            UPDATE t_files
            SET size = :size, size_hr = :size_hr, created = :created
            WHERE id = :id
            RETURNING *;
        """.trimIndent()

        return jdbi.withHandle<FileDTO?, Exception> { handle ->
            handle.createUpdate(sql)
                .bind("id", folderId)
                .bind("size", size)
                .bind("size_hr", humanReadableByteCountBin(size))
                .bind("created", created.toDate())
                .executeAndReturnGeneratedKeys()
                .mapTo<FileDTO>()
                .one()
                .also {
                    fileCache.invalidate(folderId)
                }
        }
    }

    override fun createFile(handle: Handle, file: FileDTO): FileDTO {
        val sql = """
        INSERT INTO t_files (id, name, path, mime_type, parent, owner, size, size_hr, password, created, is_folder, hash, is_root)
        VALUES (:id, :name, :path, :mime_type, :parent, :owner, :size, :size_hr, :password, :created, :is_folder, :hash, :is_root)
        RETURNING *;
    """.trimIndent()

        return try {
            handle.createUpdate(sql)
                .bind("id", file.id)
                .bind("name", file.name)
                .bind("path", file.path)
                .bind("mime_type", file.mimeType)
                .bind("parent", file.parent)
                .bind("owner", file.owner)
                .bind("size", file.size)
                .bind("size_hr", file.sizeHR)
                .bind("password", file.password)
                .bind("created", file.created?.toDate())
                .bind("is_folder", file.isFolder)
                .bind("hash", file.hash)
                .bind("is_root", file.isRoot)
                .executeAndReturnGeneratedKeys()
                .mapTo<FileDTO>()
                .one()
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
    override fun updateFile(file: FileDTO): FileDTO? {
        return try {
            jdbi.withHandle<FileDTO?, Exception> { handle ->
                updateFile(handle, file)
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not update file with id ${file.id}", e)
        }
    }

    /**
     * Updates a file in the database.
     *
     * @param handle the JDBI Handle to use for the update.
     * @param file the file to update.
     * @return the updated file or null if it does not exist.
     * @throws FileServiceException if the file could not be updated.
     */
    override fun updateFile(handle: Handle, file: FileDTO): FileDTO? {
        val sql = """
            UPDATE t_files
            SET name = :name, path = :path, mime_type = :mime_type, parent = :parent, 
                owner = :owner, size = :size, size_hr = :size_hr, password = :password, 
                created = :created, is_folder = :is_folder,hash = :hash, is_root = :is_root
            WHERE id = :id
            RETURNING *;
        """.trimIndent()

        return try {
            handle.createUpdate(sql)
                .bind("id", file.id)
                .bind("name", file.name)
                .bind("path", file.path)
                .bind("mime_type", file.mimeType)
                .bind("parent", file.parent)
                .bind("owner", file.owner)
                .bind("size", file.size)
                .bind("size_hr", file.sizeHR)
                .bind("password", file.password)
                .bind("created", file.created?.toDate())
                .bind("is_folder", file.isFolder)
                .bind("hash", file.hash)
                .bind("is_root", file.isRoot)
                .executeAndReturnGeneratedKeys()
                .mapTo<FileDTO>()
                .findOne()
                .getOrNull()
                .also {
                    fileCache.invalidate(file.id)
                }
        } catch (e: Exception) {
            throw FileServiceException("Could not update file with id ${file.id}", e)
        }
    }

    override fun updateFilesBatch(files: List<FileDTO>) {
        val sql = """
            UPDATE t_files
            SET name = :name, path = :path, mime_type = :mime_type, parent = :parent, 
                owner = :owner, size = :size, size_hr = :size_hr, password = :password, 
                created = :created, is_folder = :is_folder,hash = :hash, is_root = :is_root
            WHERE id = :id
        """.trimIndent()

        try {
            jdbi.withHandle<IntArray, Exception> { handle ->
                handle.prepareBatch(sql).use { batch ->
                    files.forEach { file ->
                        batch
                            .bind("id", file.id)
                            .bind("name", file.name)
                            .bind("path", file.path)
                            .bind("mime_type", file.mimeType)
                            .bind("parent", file.parent)
                            .bind("owner", file.owner)
                            .bind("size", file.size)
                            .bind("size_hr", file.sizeHR)
                            .bind("password", file.password)
                            .bind("created", file.created?.toDate())
                            .bind("is_folder", file.isFolder)
                            .bind("hash", file.hash)
                            .bind("is_root", file.isRoot)
                            .add()
                    }
                    batch.execute()
                        .also {
                            fileCache.invalidateAll(files.map { it.id })
                        }
                }
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not update files", e)
        }
    }


    /**
     * Fetches a list of files from the database by their IDs.
     *
     * @param fileIDs the list of UUIDs of the files to fetch.
     * @return a list of files or an empty list if no files match the provided IDs.
     * @throws FileServiceException if the files could not be fetched.
     */
    override fun getFilesByIds(fileIDs: List<UUID>): List<FileDTO> {
        val placeholders = List(fileIDs.size) { index -> ":id$index" }
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
    override fun getAllFilesFromFolderListRecursively(fileIDs: List<UUID>): List<FileDTO> {
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
            val uuidArray = conn.createArrayOf("uuid", fileIDs.map { it }.toTypedArray())

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
    override fun deleteFilesAndShares(fileIDs: List<UUID>): List<FileDTO> {
        val fileSql = """
            DELETE FROM t_files
            WHERE id = ANY(:ids)
            RETURNING *;
        """.trimIndent()

        val sharesSql = """
            DELETE FROM t_shares
            WHERE file = ANY(:ids);
        """.trimIndent()

        return try {
            val deletedFiles: MutableList<FileDTO> = mutableListOf()

            jdbi.useTransaction<Exception> { handle ->
                val conn = handle.jdbi.open().connection
                val uuidArray = conn.createArrayOf("uuid", fileIDs.map { it }.toTypedArray())

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
            }.also {
                fileCache.invalidateAll(fileIDs)
            }

            deletedFiles
        } catch (e: Exception) {
            throw FileServiceException("Could not delete files and shares with ids $fileIDs", e)
        }
    }

    /**
     * Searches for files with a given query belonging to a specific user.
     *
     * @param userId The UUID of the user whose files to search for.
     * @param query The search query to match against the file names.
     * @return A list of FileDTO objects that match the given query.
     * @throws FileServiceException If an error occurs while searching for files.
     */
    override fun searchFiles(userId: UUID, query: String, limit: Int): List<FileDTO> {
        return searchFiles(userId, query, limit, "all")
    }

    override fun searchFiles(userId: UUID, query: String, limit: Int, type: String): List<FileDTO> {
        val sql = when (type) {
            "folder" -> {
                """
                    SELECT * FROM t_files
                    WHERE owner = :owner
                    AND name ILIKE :query
                    AND is_root = false
                    AND is_folder = true
                    LIMIT :limit;
                """.trimIndent()
            }

            else -> {
                """
                    SELECT * FROM t_files
                    WHERE owner = :owner
                    AND name ILIKE :query
                    AND is_root = false
                    LIMIT :limit;
                """.trimIndent()
            }
        }

        return try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("owner", userId)
                    .bind("query", "%$query%")
                    .bind("limit", limit)
                    .mapTo<FileDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Error searching files for user with ID $userId and query $query", e)
        }
    }


    /**
     * Deletes all files owned by the given user.
     *
     * @param userId The unique identifier of the user whose files need to be deleted.
     * @throws FileServiceException if an error occurs while deleting the files.
     */
    override fun deleteAllFilesFromUser(userId: UUID): List<FileDTO> {
        getRootFolderForUser(userId)?.let { root ->
            val files = getAllFilesFromFolderListRecursively(listOf(root.id))
            return deleteFilesAndShares(files.map { it.id })
        }
        return emptyList()
    }


    /**
     * Retrieves the total number of files in the database.
     *
     * @return The total number of files.
     * @throws FileServiceException If there was an error counting the total files.
     */
    override fun countTotalFiles(): Int {
        val sql = """
            SELECT COUNT(*) FROM t_files;
        """.trimIndent()

        return try {
            jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery(sql)
                    .mapTo(Int::class.java)
                    .one()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not count total files", e)
        }
    }


    /**
     * Fetches a list of files from the database that do not have a hash value.
     *
     * @return A list of FileDTO objects representing the files without hashes.
     * @throws FileServiceException If there is an error fetching the files from the database.
     */
    override fun getFilesWithoutHashes(): List<FileDTO> {
        val sql = """
            SELECT * FROM t_files WHERE is_folder = FALSE AND is_root = FALSE and hash IS NULL;
        """.trimIndent()

        return try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .mapTo<FileDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch files without hashes", e)
        }
    }

    /**
     * Retrieves the folder tree structure for a specific user.
     *
     * @param userId The ID of the user.
     * @return A [FolderTreeStructure] object representing the folder tree structure.
     * @throws FileServiceException If there is an error retrieving the folder tree structure.
     */
    override fun getFolderTreeStructure(userId: UUID): FolderTreeStructure {
        val sql = """
            SELECT * FROM t_files WHERE owner = :owner AND is_folder = TRUE;            
        """.trimIndent()

        val sorter = FileSortingDirection.sortDirections[0]

        val folders = try {
            jdbi.withHandle<List<FileDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("owner", userId)
                    .mapTo<FileDTO>()
                    .list()
                    .sortedWith(sorter::compare)
            }
        } catch (e: Exception) {
            throw FileServiceException("Error fetching folder tree structure for user with ID $userId", e)
        }

        val folderMap = folders
            .associateBy { it.id }
            .mapValues { (id, folder) ->
                FolderTreeStructure(folder.name, id, mutableListOf())
            }

        fun addChildrenToFolder(folderStructure: FolderTreeStructure) {
            val children = folders.filter { it.parent == folderStructure.id }

            for (child in children) {
                val childStructure = folderMap[child.id]!!
                addChildrenToFolder(childStructure)
                folderStructure.folders = (folderStructure.folders + childStructure).toMutableList()
            }
        }

        val rootFolder = folders.find { it.isRoot == true }!!
        val rootFolderStructure = folderMap[rootFolder.id]!!
        addChildrenToFolder(rootFolderStructure)

        return rootFolderStructure
    }
}