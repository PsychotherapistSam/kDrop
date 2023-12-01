package de.sam.base.services

import de.sam.base.database.ShareDTO
import de.sam.base.database.jdbi
import de.sam.base.exceptions.FileServiceException
import org.jdbi.v3.core.kotlin.mapTo
import java.util.*

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
}