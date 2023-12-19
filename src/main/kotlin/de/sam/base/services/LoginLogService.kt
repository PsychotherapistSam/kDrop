package de.sam.base.services

import de.sam.base.database.LoginLogDTO
import de.sam.base.database.jdbi
import de.sam.base.utils.realIp
import io.javalin.http.Context
import org.joda.time.DateTime
import java.util.*

class LoginLogService {

    /**
     * Logs a login attempt.
     *
     * @param ctx The context of the request.
     * @param userId The unique identifier of the user.
     * @throws Exception if an error occurs while logging the login.
     */
    fun logLoginForUserId(ctx: Context, userId: UUID) {
        val sql = """
            INSERT INTO t_login_log (id, "user", ip, user_agent, date, session_id)
            VALUES (CAST(:id AS uuid), CAST(:user AS uuid), :ip, :userAgent, :date, :sessionId);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", UUID.randomUUID())
                    .bind("user", userId)
                    .bind("ip", ctx.realIp)
                    .bind("userAgent", ctx.userAgent()!!)
                    .bind("date", DateTime.now().toDate())
                    .bind("sessionId", ctx.req().session.id)
                    .execute()
            }
        } catch (e: Exception) {
            throw Exception("Could not log login for user $userId", e)
        }
    }

    /**
     * Retrieves the login history for a given user.
     *
     * @param userId The unique identifier of the user.
     * @return The list of login logs associated with the user.
     * @throws Exception if an error occurs while fetching the login logs.
     */
    fun getLoginHistoryByUserId(userId: UUID): List<LoginLogDTO> {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        try {
            return jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        } catch (e: Exception) {
            throw Exception("Could not get login logs for user $userId", e)
        }
    }

    fun getLimitedLoginHistoryByUserId(userId: UUID, days: Int): List<LoginLogDTO> {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid)
            AND date > NOW() - INTERVAL '$days days';
        """.trimIndent()

        try {
            return jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        } catch (e: Exception) {
            throw Exception("Could not get login logs for user $userId", e)
        }
    }

    /**
     * Deletes all login logs for a given user.
     *
     * @param userId The unique identifier of the user.
     * @throws Exception if an error occurs while deleting the login logs.
     */
    fun deleteAllLoginLogsForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("userId", userId.toString())
                    .execute()
            }
        } catch (e: Exception) {
            throw Exception("Could not delete delete login log for user $userId", e)
        }
    }

    /**
     * Retrieves the login log for a given session.
     *
     * @param sessionId The unique identifier of the session.
     * @return The login log associated with the session.
     * @throws Exception if an error occurs while fetching the login log.
     */
    fun getLogBySessionId(sessionId: String): LoginLogDTO? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE session_id = :sessionId;
        """.trimIndent()

        try {
            return jdbi.withHandle<LoginLogDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("sessionId", sessionId)
                    .mapTo(LoginLogDTO::class.java)
                    .findFirst()
                    .orElse(null)
            }
        } catch (e: Exception) {
            throw Exception("Could not get login logs for session $sessionId", e)
        }
    }

    /**
     * Retrieves the login log for a given id.
     *
     * @param id The unique identifier of the login log.
     * @return The login log associated with the id.
     * @throws Exception if an error occurs while fetching the login log.
     */
    fun getLogById(id: UUID): LoginLogDTO? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        try {
            return jdbi.withHandle<LoginLogDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .findFirst()
                    .orElse(null)
            }
        } catch (e: Exception) {
            throw Exception("Could not get login log for id $id", e)
        }
    }

    /**
     * Deletes a login log entry.
     *
     * @param id The unique identifier of the login log.
     * @throws Exception if an error occurs while deleting the login log.
     */
    fun removeLogEntry(id: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", id.toString())
                    .execute()
            }
        } catch (e: Exception) {
            throw Exception("Could not delete login log entry $id", e)
        }
    }

    /**
     * Updates a login log entry.
     *
     * @param loginLog The login log to update.
     * @throws Exception if an error occurs while updating the login log.
     */
    fun updateLoginLogEntry(loginLog: LoginLogDTO) {
        val sql = """
            UPDATE t_login_log
            SET "user" = CAST(:user AS uuid),
                ip = :ip,
                user_agent = :userAgent,
                date = :date,
                session_id = :sessionId,
                revoked = :revoked
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        try {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("user", loginLog.user.toString())
                    .bind("ip", loginLog.ip)
                    .bind("userAgent", loginLog.userAgent)
                    .bind("date", loginLog.date.toDate())
                    .bind("sessionId", loginLog.sessionId)
                    .bind("id", loginLog.id.toString())
                    .bind("revoked", loginLog.revoked)
                    .execute()
            }
        } catch (e: Exception) {
            throw Exception("Could not update login log entry ${loginLog.id}", e)
        }
    }
}