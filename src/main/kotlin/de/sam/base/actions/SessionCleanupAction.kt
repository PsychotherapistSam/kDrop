package de.sam.base.actions

import de.sam.base.authentication.UserService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SessionCleanupAction : KoinComponent {

    private val userService: UserService by inject()

    fun cleanup() {
        userService.deleteAllSessions()
    }
}