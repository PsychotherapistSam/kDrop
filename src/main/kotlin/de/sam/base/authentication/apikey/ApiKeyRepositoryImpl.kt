package de.sam.base.authentication.apikey

import de.sam.base.database.ApiKeyDTO
import de.sam.base.database.UserDTO
import de.sam.base.database.jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ApiKeyRepositoryImpl : ApiKeyRepository {
    override fun getApiKeyById(id: UUID): ApiKeyDTO? {
        val sql = """
            SELECT * FROM t_api_keys
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching API key by ID $id") {
            jdbi.withHandle<ApiKeyDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo<ApiKeyDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    override fun getApiKeyByApiKey(apiKey: String): ApiKeyDTO? {
        val sql = """
            SELECT * FROM t_api_keys
            WHERE api_key = :apiKey;
        """.trimIndent()

        return executeWithExceptionHandling("fetching API key by API key") {
            jdbi.withHandle<ApiKeyDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("apiKey", apiKey)
                    .mapTo<ApiKeyDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    override fun getUserForApiKey(apiKey: String): UserDTO? {
        val sql = """
            SELECT * FROM t_users
            WHERE id = (
                SELECT user_id FROM t_api_keys
                WHERE api_key = :apiKey
            );
        """.trimIndent()

        return executeWithExceptionHandling("fetching user for API key") {
            jdbi.withHandle<UserDTO, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("apiKey", apiKey)
                    .mapTo<UserDTO>()
                    .findOne()
                    .getOrNull()
            }
        }
    }

    override fun getApiKeysForUser(userId: UUID): List<ApiKeyDTO> {
        val sql = """
            SELECT * FROM t_api_keys
            WHERE user_id = CAST(:userId AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching all API keys for user $userId") {
            jdbi.withHandle<List<ApiKeyDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo<ApiKeyDTO>()
                    .list()
            }
        } ?: emptyList()
    }

    override fun createApiKey(apiKey: ApiKeyDTO): ApiKeyDTO? {
        val sql = """
            INSERT INTO t_api_keys (id, api_key, user_id, created_at)
            VALUES (:id, :apiKey, :userId, :createdAt);
        """.trimIndent()

        return executeWithExceptionHandling("creating API key") {
            jdbi.withHandle<ApiKeyDTO, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", apiKey.id)
                    .bind("apiKey", apiKey.apiKey)
                    .bind("userId", apiKey.user)
                    .bind("createdAt", apiKey.createdAt.toDate())
                    .executeAndReturnGeneratedKeys()
                    .mapTo<ApiKeyDTO>()
                    .one()
            }
        }
    }

    override fun deleteApiKey(apiKeyId: UUID?) {
        val sql = """
            DELETE FROM t_api_keys
            WHERE id = :id;
        """.trimIndent()

        executeWithExceptionHandling("deleting API key") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", apiKeyId)
                    .execute()
            }
        }
    }
}