package de.sam.base.user.integrations

import de.sam.base.database.FileDTO
import de.sam.base.database.jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.koin.core.component.KoinComponent
import java.util.*

class IntegrationRepositoryImpl : IntegrationRepository, KoinComponent {

    override fun getShareXFolderForUser(userId: UUID): FileDTO? {
        val sql = """
            SELECT * FROM t_files
            WHERE id = (
                SELECT folder_id FROM t_sharex_integration
                WHERE user_id = :user_id
            );
        """.trimIndent()

        return executeWithExceptionHandling("fetching ShareX folder for user") {
            jdbi.withHandle<FileDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("user_id", userId)
                    .mapTo<FileDTO>()
                    .findOne()
                    .orElse(null)
            }
        }
    }

    override fun setShareXFolderForUser(userId: UUID, folderId: UUID): Boolean {
        val sql = """
            INSERT INTO t_sharex_integration (user_id, folder_id)
            VALUES (:user_id, :folder_id)
            ON CONFLICT (user_id)
            DO UPDATE SET folder_id = :folder_id;
        """.trimIndent()

        return executeWithExceptionHandling("setting ShareX folder for user") {
            jdbi.withHandle<Int, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("user_id", userId)
                    .bind("folder_id", folderId)
                    .execute()
            }
        } != 0
    }

    override fun disableShareXFolderForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_sharex_integration
            WHERE user_id = :user_id;
        """.trimIndent()

        executeWithExceptionHandling("disabling ShareX folder for user") {
            jdbi.withHandle<Int, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("user_id", userId)
                    .execute()
            }
        }
    }
}