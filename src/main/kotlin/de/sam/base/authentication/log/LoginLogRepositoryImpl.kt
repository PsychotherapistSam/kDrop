package de.sam.base.authentication.log

import de.sam.base.database.LoginLogDTO
import de.sam.base.database.jdbi
import de.sam.base.utils.realIp
import io.javalin.http.Context
import org.joda.time.DateTime
import java.util.*

class LoginLogRepositoryImpl : LoginLogRepository {

    override fun logLoginForUserId(ctx: Context, userId: UUID, date: DateTime) {
        val sql = """
            INSERT INTO t_login_log (id, "user", ip, user_agent, date, session_id)
            VALUES (CAST(:id AS uuid), CAST(:user AS uuid), :ip, :userAgent, :date, :sessionId);
        """.trimIndent()

        executeWithExceptionHandling("logging login for user $userId") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", UUID.randomUUID())
                    .bind("user", userId)
                    .bind("ip", ctx.realIp)
                    .bind("userAgent", ctx.userAgent()!!)
                    .bind("date", date.toDate())
                    .bind("sessionId", ctx.req().session.id)
                    .execute()
            }
        }
    }

    override fun getLoginHistoryByUserId(userId: UUID): List<LoginLogDTO>? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching login logs for user $userId") {
            jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        }
    }

    override fun getLimitedLoginHistoryByUserId(userId: UUID, days: Int): List<LoginLogDTO>? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid)
            AND date > NOW() - INTERVAL '$days days';
        """.trimIndent()

        return executeWithExceptionHandling("fetching limited login logs for user $userId") {
            jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        }
    }

    override fun deleteAllLoginLogsForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE "user" = CAST(:userId AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("deleting login logs for user $userId") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("userId", userId.toString())
                    .execute()
            }
        }
    }

    override fun getLogBySessionId(sessionId: String): LoginLogDTO? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE session_id = :sessionId;
        """.trimIndent()

        return executeWithExceptionHandling("fetching login logs for session $sessionId") {
            jdbi.withHandle<LoginLogDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("sessionId", sessionId)
                    .mapTo(LoginLogDTO::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }
    }

    override fun getLogById(id: UUID): LoginLogDTO? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        return executeWithExceptionHandling("fetching login logs for id $id") {
            jdbi.withHandle<LoginLogDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id.toString())
                    .mapTo(LoginLogDTO::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }
    }

    override fun removeLogEntry(id: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE id = CAST(:id AS uuid);
        """.trimIndent()

        executeWithExceptionHandling("deleting login log entry $id") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", id.toString())
                    .execute()
            }
        }
    }

    override fun updateLoginLogEntry(loginLog: LoginLogDTO) {
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

        executeWithExceptionHandling("updating login log entry ${loginLog.id}") {
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
        }
    }
}