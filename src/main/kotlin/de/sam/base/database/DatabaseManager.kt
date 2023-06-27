package de.sam.base.database

import com.zaxxer.hikari.HikariDataSource
import de.sam.base.config.Configuration.Companion.config
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

var hikariDataSource: HikariDataSource? = null

class DatabaseManager {
    fun start() {
        hikariDataSource = HikariDataSource()
        hikariDataSource!!.jdbcUrl =
            "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.database}"
        hikariDataSource!!.username = config.database.username
        hikariDataSource!!.password = config.database.password

        Database.connect(hikariDataSource!!)

        transaction {
            // print sql to std-out
            addLogger(StdOutSqlLogger)
            // create users table
            SchemaUtils.create(UsersTable)
            SchemaUtils.create(FilesTable)
            SchemaUtils.create(DownloadLogTable)
            SchemaUtils.create(SharesTable)
            SchemaUtils.create(LoginLogTable)
        }
    }
}