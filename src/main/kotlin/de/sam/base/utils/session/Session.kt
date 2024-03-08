package de.sam.base.utils.session

import de.sam.base.config.Configuration
import de.sam.base.database.hikariDataSource
import org.eclipse.jetty.server.session.DatabaseAdaptor
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory
import org.eclipse.jetty.server.session.SessionHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Session : KoinComponent {
    val sessionHandler: SessionHandler
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
}