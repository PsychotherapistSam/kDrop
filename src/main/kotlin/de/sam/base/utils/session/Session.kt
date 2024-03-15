package de.sam.base.utils.session

import de.sam.base.authentication.log.LoginLogRepository
import de.sam.base.config.Configuration
import de.sam.base.database.UserDTO
import de.sam.base.database.hikariDataSource
import de.sam.base.utils.logging.logTimeSpent
import org.eclipse.jetty.server.session.DatabaseAdaptor
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory
import org.eclipse.jetty.server.session.SessionHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.*

class Session : KoinComponent {
    val sessionHandler: SessionHandler
    private val loginLogRepository: LoginLogRepository by inject()
    val config: Configuration by inject()

    init {
        sessionHandler = SessionHandler().apply {
            sessionCache = DefaultSessionCache(this).apply { // use NullSessionCache when running multiple instances
                sessionDataStore = JDBCSessionDataStoreFactory().apply {
                    setDatabaseAdaptor(DatabaseAdaptor().apply {
                        datasource = hikariDataSource
                    })
                }.getSessionDataStore(sessionHandler)
            }

            // Session cookies are secure and httpOnly
            if (!config.devEnvironment) {
                sessionCookieConfig.isHttpOnly = true
                sessionCookieConfig.isSecure = true
            }

            // Sessions are valid for 5 days
            maxInactiveInterval = 60 * 60 * 24 * 5 // 5 days
            sessionCookieConfig.maxAge = 60 * 60 * 24 * 30 // 30 days
        }
    }

    fun forceUpdateUserSessionObject(userId: UUID, newUserData: UserDTO): Boolean {
        Logger.tag("Sessions").info("Forcing update of user session object for user $userId")

        val userSessions = getUserSessions(userId) ?: return false

        Logger.tag("Sessions").info("Found ${userSessions.size} active sessions for user $userId")

        userSessions.forEach {
            it.setAttribute("user", newUserData)
//            sessionHandler.sessionCache.commit(it)
            sessionHandler.sessionCache.release(it.id, it)

        }
        return true
    }

    fun invalidateAllSessions(userId: UUID): Boolean {
        Logger.tag("Sessions").info("Invalidating all sessions for user $userId")

        val userSessions = getUserSessions(userId) ?: return false

        Logger.tag("Sessions").info("Found ${userSessions.size} active sessions for user $userId")

        userSessions.forEach {
            it.invalidate()
//            sessionHandler.sessionCache.commit(it)
            sessionHandler.sessionCache.release(it.id, it)
        }
        return true
    }

    private fun getUserSessions(userId: UUID): List<org.eclipse.jetty.server.session.Session>? {
        val userSessions = logTimeSpent("Getting user sessions", "Sessions") {
            loginLogRepository.getLoginHistoryByUserId(userId)
                ?.filter { !it.revoked }
                ?.mapNotNull { it.sessionId }
                ?.mapNotNull { sessionHandler.getSession(it) }
                ?.filter { it.isValid }
                ?: return null
        }
        return userSessions
    }
}