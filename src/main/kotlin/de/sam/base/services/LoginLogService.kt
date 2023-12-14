package de.sam.base.services

import de.sam.base.database.*
import de.sam.base.utils.realIp
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class LoginLogService {
    fun logLogin(ctx: Context, user: UserDTO) {
        transaction {
            LoginLogDAO.new {
                this.user = UserDAO.findById(user.id)!!
                this.ip = ctx.realIp
                this.userAgent = ctx.userAgent()!!
                this.date = DateTime.now()
                this.sessionId = ctx.req().session.id
            }
        }
    }

    fun getLoginHistory(user: UserDTO): List<LoginLogDTO> {
        return transaction {
            LoginLogDAO.find { LoginLogTable.user eq user.id }
                .map { it.toDTO() }
                .sortedBy { it.date }
                .reversed()
        }
    }

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