package de.sam.base.tasks.types.user

import de.sam.base.tasks.types.Task
import de.sam.base.user.UserRepository
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger

class SessionCleanupTask : Task(name = "File hashing", concurrency = 1), KoinComponent {
    private val userRepository: UserRepository by inject()
    override suspend fun execute() {
        Logger.warn("Cleaning up all sessions")
        userRepository.deleteAllSessions()
        delay(2000)
    }
}