package de.sam.base.controllers

import de.sam.base.services.LoginLogService
import io.javalin.http.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthenticationController : KoinComponent {

    private val loginLogService: LoginLogService by inject()

    fun logoutRequest(ctx: Context) {
        val logEntry = loginLogService.getLogBySessionId(ctx.req().session.id)

        ctx.req().session.invalidate()

        if (logEntry != null)
            loginLogService.updateLoginLogEntry(logEntry.copy(sessionId = null))
    }
}
