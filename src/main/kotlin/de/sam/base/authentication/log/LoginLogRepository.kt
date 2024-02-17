package de.sam.base.authentication.log

import de.sam.base.database.LoginLogDTO
import io.javalin.http.Context
import org.joda.time.DateTime
import java.util.*

interface LoginLogRepository {
    fun logLoginForUserId(ctx: Context, userId: UUID, date: DateTime)
    fun getLoginHistoryByUserId(userId: UUID): List<LoginLogDTO>
    fun getLimitedLoginHistoryByUserId(userId: UUID, days: Int): List<LoginLogDTO>
    fun deleteAllLoginLogsForUser(userId: UUID)
    fun getLogBySessionId(sessionId: String): LoginLogDTO?
    fun getLogById(id: UUID): LoginLogDTO?
    fun removeLogEntry(id: UUID)
    fun updateLoginLogEntry(loginLog: LoginLogDTO)
}