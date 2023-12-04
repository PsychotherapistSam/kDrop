package de.sam.base.services

import de.sam.base.database.ShareDTO
import de.sam.base.database.jdbi
import de.sam.base.exceptions.FileServiceException
import org.jdbi.v3.core.kotlin.mapTo
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ShareService {
    fun getAllSharesForUser(userId: UUID): List<ShareDTO> {
        val sql = """
            SELECT * FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        return try {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch shares for user $userId", e)
        }
    }

    fun deleteAllSharesForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("userId", userId.toString())
                    .execute()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not delete shares for user $userId", e)
        }
    }


    /**
     * Retrieves the list of shares for a given file.
     *
     * @param id The unique identifier of the file.
     * @return The list of shares associated with the file.
     * @throws FileServiceException if an error occurs while fetching the shares.
     */
    fun getSharesForFile(id: UUID): List<ShareDTO> {
        val sql = """
            SELECT * FROM t_shares
            WHERE "file" = CAST(:id AS uuid);
        """.trimIndent()

        return try {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch shares for file $id", e)
        }
    }

    /**
     * Creates a new share in the database.
     *
     * @param share the share to be created
     * @return the created share
     * @throws FileServiceException if the share creation fails
     */
    fun createShare(share: ShareDTO): ShareDTO {
        val sql = """
            INSERT INTO t_shares (id, file, "user", creation_date, max_downloads, download_count, vanity_name, password)
            VALUES (CAST(:id AS uuid), CAST(:file AS uuid), CAST(:user AS uuid), :creationDate, :maxDownloads, :downloadCount, :vanityName, :password)
            RETURNING *;
        """.trimIndent()

        return try {
            jdbi.withHandle<ShareDTO, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", share.id.toString())
                    .bind("file", share.file.toString())
                    .bind("user", share.user.toString())
                    .bind("creationDate", share.creationDate.toDate())
                    .bind("maxDownloads", share.maxDownloads)
                    .bind("downloadCount", share.downloadCount)
                    .bind("vanityName", share.vanityName)
                    .bind("password", share.password)
                    .executeAndReturnGeneratedKeys()
                    .mapTo<ShareDTO>()
                    .one()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not create share for file ${share.file}", e)
        }
    }

    /**
     * Retrieves a share by its name.
     *
     * @param name the name of the share
     * @return the ShareDTO object representing the share with the given name, or null if not found
     * @throws FileServiceException if there is an error while fetching the share
     */
    fun getShareByName(name: String): ShareDTO? {
        val sql = """
            SELECT * FROM t_shares
            WHERE vanity_name = :name;
        """.trimIndent()

        return try {
            jdbi.withHandle<ShareDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("name", name)
                    .mapTo<ShareDTO>()
                    .findOne()
                    .getOrNull()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch share for name $name", e)
        }
    }

    /**
     * Deletes a share from the database.
     *
     * @param id The ID of the share to be deleted.
     * @throws FileServiceException if the share could not be deleted.
     */
    fun deleteShare(id: UUID) {
        val sql = """
            DELETE FROM t_shares
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", id.toString())
                    .execute()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not delete share $id", e)
        }
    }

    /**
     * Retrieves a list of shares for the given user.
     *
     * @param userId The ID of the user.
     * @return The list of ShareDTO objects representing the shares for the user.
     * @throws FileServiceException if an error occurs while fetching the shares.
     */
    fun getSharesForUser(userId: UUID): List<ShareDTO> {
        val sql = """
            SELECT * FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        return try {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        } catch (e: Exception) {
            throw FileServiceException("Could not fetch shares for user $userId", e)
        }
    }
}