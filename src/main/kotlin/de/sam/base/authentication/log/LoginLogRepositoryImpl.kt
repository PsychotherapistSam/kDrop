package de.sam.base.authentication.log

import de.sam.base.database.LoginLogDTO
import de.sam.base.database.jdbi
import de.sam.base.user.UserRepository
import de.sam.base.utils.realIp
import io.javalin.http.Context
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class LoginLogRepositoryImpl : LoginLogRepository, KoinComponent {
    private val userRepository: UserRepository by inject()

    override fun logLoginForUserId(ctx: Context, userId: UUID, date: DateTime, failed: Boolean) {
        if (!failed)
            userRepository.updateLastLoginTime(userId, date)

        val sql = """
            INSERT INTO t_login_log (id, "user", ip, user_agent, date, session_id, failed)
            VALUES (:id, :user, :ip, :userAgent, :date, :sessionId, :failed);
        """.trimIndent()

        executeWithExceptionHandling("logging login for user $userId") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", UUID.randomUUID())
                    .bind("user", userId)
                    .bind("ip", ctx.realIp)
                    .bind("userAgent", ctx.userAgent()!!)
                    .bind("date", date.toDate())
                    .bind("sessionId", if (!failed) ctx.req().session.id else null)
                    .bind("failed", failed)
                    .execute()
            }
        }
    }

    override fun getLoginHistoryByUserId(userId: UUID): List<LoginLogDTO>? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = :userId;
        """.trimIndent()

        return executeWithExceptionHandling("fetching login logs for user $userId") {
            jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId)
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        }
    }

    override fun getLimitedLoginHistoryByUserId(userId: UUID, days: Int): List<LoginLogDTO>? {
        val sql = """
            SELECT * FROM t_login_log
            WHERE "user" = :userId
            AND date > NOW() - INTERVAL '$days days';
        """.trimIndent()

        return executeWithExceptionHandling("fetching limited login logs for user $userId") {
            jdbi.withHandle<List<LoginLogDTO>, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("userId", userId)
                    .mapTo(LoginLogDTO::class.java)
                    .list()
            }
        }
    }

    override fun deleteAllLoginLogsForUser(userId: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE "user" = :userId;
        """.trimIndent()

        executeWithExceptionHandling("deleting login logs for user $userId") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("userId", userId)
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
            WHERE id = :id;
        """.trimIndent()

        return executeWithExceptionHandling("fetching login logs for id $id") {
            jdbi.withHandle<LoginLogDTO?, Exception> { handle ->
                handle.createQuery(sql)
                    .bind("id", id)
                    .mapTo(LoginLogDTO::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }
    }

    override fun removeLogEntry(id: UUID) {
        val sql = """
            DELETE FROM t_login_log
            WHERE id = :id;
        """.trimIndent()

        executeWithExceptionHandling("deleting login log entry $id") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("id", id)
                    .execute()
            }
        }
    }

    override fun updateLoginLogEntry(loginLog: LoginLogDTO) {
        val sql = """
            UPDATE t_login_log
            SET "user" = :user,
                ip = :ip,
                user_agent = :userAgent,
                date = :date,
                session_id = :sessionId,
                revoked = :revoked,
                failed = :failed
            WHERE id = :id;
        """.trimIndent()

        executeWithExceptionHandling("updating login log entry ${loginLog.id}") {
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(sql)
                    .bind("user", loginLog.user)
                    .bind("ip", loginLog.ip)
                    .bind("userAgent", loginLog.userAgent)
                    .bind("date", loginLog.date.toDate())
                    .bind("sessionId", loginLog.sessionId)
                    .bind("id", loginLog.id)
                    .bind("revoked", loginLog.revoked)
                    .bind("failed", loginLog.failed)
                    .execute()
            }
        }
    }
}