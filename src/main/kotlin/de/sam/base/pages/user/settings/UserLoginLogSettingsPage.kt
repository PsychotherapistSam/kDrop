package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.authentication.log.LoginLogRepository
import de.sam.base.database.LoginLogDTO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.session.Session
import io.javalin.http.HttpStatus
import org.koin.core.component.inject
import java.util.*


class UserLoginLogSettingsPage : Page(
    name = "Login History",
    templateName = "user/settings/login_log.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/loginHistory"
        const val LOGIN_LOG_DAYS: Int = 30
    }

    private val session: Session by inject()
    private val loginLogRepository: LoginLogRepository by inject()

    var loginLogList = listOf<LoginLogDTO>()

    var errors = arrayListOf<String>()

    override fun get() {
        loginLogList = loadLoginList()
    }

    override fun post() {
        val logId = UUID.fromString(ctx.formParam("logId"))

        loginLogList = loadLoginList()

        if (logId == null) {
            errors.add("Log not found.")
            return
        }

        val log = loginLogRepository.getLogById(logId)

        if (log == null || log.user != ctx.currentUserDTO!!.id) {
            errors.add("Log not found.")
            return
        }

        if (log.sessionId == null) {
            return
        }

        session.sessionHandler.invalidate(log.sessionId)
        loginLogRepository.updateLoginLogEntry(log.copy(revoked = true))

        loginLogList = loadLoginList()

        ctx.status(HttpStatus.OK)
    }

    private fun loadLoginList(): List<LoginLogDTO> {
        return loginLogRepository.getLimitedLoginHistoryByUserId(ctx.currentUserDTO!!.id, LOGIN_LOG_DAYS)
            .sortedBy { it.date }
            .reversed()
    }
}
