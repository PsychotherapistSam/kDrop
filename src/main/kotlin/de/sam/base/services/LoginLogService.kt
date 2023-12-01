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
}