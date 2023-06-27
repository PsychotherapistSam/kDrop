package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.database.LoginLogDTO
import de.sam.base.services.LoginLogService
import de.sam.base.utils.currentUserDTO


class UserLoginLogSettingsPage(private val loginLogService: LoginLogService) : Page(
    name = "Login History",
    templateName = "user/settings/login_log.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    var loginLogList = listOf<LoginLogDTO>()

    override fun before() {
        loginLogList = ArrayList()
    }

    override fun get() {
        loginLogList = loginLogService.getLoginHistory(ctx.currentUserDTO!!)
    }
}
