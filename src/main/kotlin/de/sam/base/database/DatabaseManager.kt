package de.sam.base.database

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class DatabaseManager {
    fun start() {
        val ds = HikariDataSource()
        ds.jdbcUrl = "jdbc:postgresql://db.tcbipcnpdbvkdydnnzbk.supabase.co:5432/postgres"
        ds.username = "postgres"
        ds.password = "supersecretdatabasepasswordonlyusedforthisproject"

        Database.connect(ds)

        transaction {
            // print sql to std-out
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Users)

            // create default user if not exists
            if (Users.select { Users.name eq "Samuel" }.count() == 0) {
                User.new {
                    name = "Samuel"
                    password = "supersecretpassword"
                    roles = "0,1,2"
                    hidden = false
                    preferences = "{\"language\":\"en\"}"
                    registrationDate = DateTime.now()
                }
            }
        }
    }


    object Users : UUIDTable("t_users") {
        val name = varchar("name", 50)
        val password = varchar("password", 50)
        val roles = varchar("roles", 50)
        val hidden = bool("hidden")
        val preferences = varchar("preferences", 256)
        val registrationDate = datetime("registration_date")
    }

    class User(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<User>(Users)

        var name by Users.name
        var password by Users.password
        var roles by Users.roles
        var hidden by Users.hidden
        var preferences by Users.preferences
        var registrationDate by Users.registrationDate

        fun hasRole(role: String): Boolean {
            return roles.split(",").contains(role)
        }

        fun hasPreferences(preference: String): Boolean {
            return this.preferences.split(",").contains(preferences)
        }

        fun getPreference(preference: String): String? {
            val prefs = preferences.split(",")
            val index = prefs.indexOf(preference)
            if (index == -1) {
                return null
            }
            return prefs[index + 1]
        }
    }
}