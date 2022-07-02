package de.sam.base.utils.session

import de.sam.base.database.hikariDataSource
import org.eclipse.jetty.server.session.*
import java.io.File

object Session {
    fun fileSessionHandler() = SessionHandler().apply {
        httpOnly = false
        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = FileSessionDataStore().apply {
                val baseDir = File(System.getProperty("java.io.tmpdir"))
                this.storeDir = File(baseDir, "javalin-session-store").apply { mkdir() }
            }
        }
    }

    fun sqlSessionHandler() = SessionHandler().apply {
        sessionCache = DefaultSessionCache(this).apply { // create the session handler
            sessionDataStore = JDBCSessionDataStoreFactory().apply { // attach a cache to the handler
                setDatabaseAdaptor(DatabaseAdaptor().apply { // attach a store to the cache
                  //  setDriverInfo(driver, url)
                    datasource = hikariDataSource!! // you can set data source here (for connection pooling, etc)
                })
            }.getSessionDataStore(sessionHandler)
        }
        httpOnly = true

        // Sessions are valid for 5 days
        maxInactiveInterval = 60 * 60 * 24 * 5 // 5 days
    }
}