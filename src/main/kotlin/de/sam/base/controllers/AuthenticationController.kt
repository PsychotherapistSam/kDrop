package de.sam.base.controllers

import de.sam.base.authentication.log.LoginLogRepository
import io.javalin.http.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthenticationController : KoinComponent {

    private val loginLogRepository: LoginLogRepository by inject()

    fun logoutRequest(ctx: Context) {
        val logEntry = loginLogRepository.getLogBySessionId(ctx.req().session.id)

        ctx.req().session.invalidate()

        if (logEntry != null)
            loginLogRepository.updateLoginLogEntry(logEntry.copy(sessionId = null))
    }
}
