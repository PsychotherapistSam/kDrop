package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.database.LoginLogDTO
import de.sam.base.services.LoginLogService
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.session.Session
import org.koin.core.component.inject
import java.util.*


class UserLoginLogSettingsPage : Page(
    name = "Login History",
    templateName = "user/settings/login_log.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/loginHistory"
    }

    private val session: Session by inject()
    private val loginLogService: LoginLogService by inject()

    var loginLogList = listOf<LoginLogDTO>()

    var errors = arrayListOf<String>()

    override fun get() {
        loginLogList = loginLogService.getLoginHistoryByUserId(ctx.currentUserDTO!!.id)
            .sortedBy { it.date }
            .reversed()
    }

    override fun post() {
        val logId = UUID.fromString(ctx.formParam("logId"))

        loginLogList = loginLogService.getLoginHistoryByUserId(ctx.currentUserDTO!!.id)
            .sortedBy { it.date }
            .reversed()

        if (logId == null) {
            errors.add("Log not found.")
            return
        }

        val log = loginLogService.getLogById(logId)

        if (log == null || log.user != ctx.currentUserDTO!!.id) {
            errors.add("Log not found.")
            return
        }

        if (log.sessionId == null) {
            return
        }

        session.sessionHandler.invalidate(log.sessionId)
        loginLogService.updateLoginLogEntry(log.copy(revoked = true))

        loginLogList = loginLogService.getLoginHistoryByUserId(ctx.currentUserDTO!!.id)
            .sortedBy { it.date }
            .reversed()

        ctx.status(200)
    }
}
