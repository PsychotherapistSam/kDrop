package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.database.LoginLogDTO
import de.sam.base.services.LoginLogService
import de.sam.base.utils.currentUserDTO
import org.koin.core.component.inject


class UserLoginLogSettingsPage() : Page(
    name = "Login History",
    templateName = "user/settings/login_log.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/loginHistory"
    }

    private val loginLogService: LoginLogService by inject()


    var loginLogList = listOf<LoginLogDTO>()

    override fun before() {
        loginLogList = ArrayList()
    }

    override fun get() {
        loginLogList = loginLogService.getLoginHistory(ctx.currentUserDTO!!)
    }
}
