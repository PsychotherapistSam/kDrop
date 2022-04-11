package de.sam.base.database

import com.zaxxer.hikari.HikariDataSource
import de.sam.base.users.UserRoles
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import de.sam.base.config.Configuration.Companion.config

class DatabaseManager {
    fun start() {
        val hikariDataSource = HikariDataSource()
        hikariDataSource.jdbcUrl =
            "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.database}"
        hikariDataSource.username = config.database.username
        hikariDataSource.password = config.database.password

        Database.connect(hikariDataSource)

        transaction {
            // print sql to std-out
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(UsersTable)

            // create default user if not exists
            /*if (UsersTable.select { UsersTable.name eq "Samuel" }.count() == 0) {
                User.new {
                    name = "Samuel"
                    password = "supersecretpassword"
                    roles = "0,1"
                    hidden = false
                    preferences = "{\"language\":\"en\"}"
                    registrationDate = DateTime.now()
                }
            }*/
        }
    }
}