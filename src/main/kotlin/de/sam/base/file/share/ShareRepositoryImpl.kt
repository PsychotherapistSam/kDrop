package de.sam.base.file.share

import de.sam.base.database.ShareDTO
import de.sam.base.database.jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ShareRepositoryImpl : ShareRepository {
    override fun getAllSharesForUser(userId: UUID): List<ShareDTO>? {
        val sql = """
            SELECT * FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching shares for user $userId") {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        }
    }

    override fun deleteAllSharesForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("deleting shares for user $userId") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("userId", userId.toString())
                    .execute()
            }
        }
    }

    override fun getSharesForFile(id: UUID): List<ShareDTO>? {
        val sql = """
            SELECT * FROM t_shares
            WHERE "file" = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching shares for file $id") {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        }
    }

    override fun createShare(share: ShareDTO): ShareDTO? {
        val sql = """
            INSERT INTO t_shares (id, file, "user", creation_date, max_downloads, download_count, vanity_name, password)
            VALUES (CAST(:id AS uuid), CAST(:file AS uuid), CAST(:user AS uuid), :creationDate, :maxDownloads, :downloadCount, :vanityName, :password)
            RETURNING *;
        """.trimIndent()

        return executeWithExceptionHandling("creating share for file ${share.file}") {
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
        }
    }

    override fun getShareByName(name: String): ShareDTO? {
        val sql = """
            SELECT * FROM t_shares
            WHERE vanity_name = :name;
        """.trimIndent()

        return executeWithExceptionHandling("fetching share for name $name") {
            jdbi.withHandle<ShareDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("name", name)
                    .mapTo<ShareDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    override fun getShareById(id: UUID): ShareDTO? {
        val sql = """
            SELECT * FROM t_shares
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching share for id $id") {
            jdbi.withHandle<ShareDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo<ShareDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    override fun deleteShare(id: UUID) {
        val sql = """
            DELETE FROM t_shares
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("deleting share $id") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", id.toString())
                    .execute()
            }
        }
    }

    override fun getSharesForUser(userId: UUID): List<ShareDTO>? {
        val sql = """
            SELECT * FROM t_shares
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching shares for user $userId") {
            jdbi.withHandle<List<ShareDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo<ShareDTO>()
                    .list()
            }
        }
    }

    override fun updateShare(share: ShareDTO) {
        val sql = """
            UPDATE t_shares
            SET max_downloads = :maxDownloads, download_count = :downloadCount, vanity_name = :vanityName, password = :password
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("updating share ${share.id}") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", share.id.toString())
                    .bind("maxDownloads", share.maxDownloads)
                    .bind("downloadCount", share.downloadCount)
                    .bind("vanityName", share.vanityName)
                    .bind("password", share.password)
                    .execute()
            }
        }
    }

    override fun updateShareDownloadCount(share: ShareDTO) {
        val sql = """
            UPDATE t_shares
            SET download_count = :downloadCount
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("updating download count for share ${share.id}") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", share.id.toString())
                    .bind("downloadCount", share.downloadCount)
                    .execute()
            }
        }
    }
}