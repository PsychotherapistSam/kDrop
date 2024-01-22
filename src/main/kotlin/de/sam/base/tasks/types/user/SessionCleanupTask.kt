package de.sam.base.tasks.types.user

import de.sam.base.authentication.UserService
import de.sam.base.tasks.types.Task
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger

class SessionCleanupTask : Task(name = "File hashing", concurrency = 1), KoinComponent {
    private val userService: UserService by inject()
    override suspend fun execute() {
        Logger.warn("Cleaning up all sessions")
        userService.deleteAllSessions()
        delay(2000)
    }
}