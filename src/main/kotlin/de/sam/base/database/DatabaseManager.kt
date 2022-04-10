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


    object UsersTable : UUIDTable("t_users") {
        val name = varchar("name", 50)
        val password = varchar("password", 256)
        val roles = varchar("roles", 50)
        val hidden = bool("hidden")
        val preferences = varchar("preferences", 256)
        val registrationDate = datetime("registration_date")
    }

    class User(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<User>(UsersTable)

        var name by UsersTable.name
        var password by UsersTable.password
        var roles by UsersTable.roles
        var hidden by UsersTable.hidden
        var preferences by UsersTable.preferences
        var registrationDate by UsersTable.registrationDate

        fun getRolesAsEnum(): List<UserRoles> {
            return roles.split(",").map { it.toInt() }.map { UserRoles.values()[it] }
        }

        fun getHighestRole(): UserRoles {
            return getRolesAsEnum().maxByOrNull { it.powerLevel }!! // users should not not have a role
        }

        fun hasRole(role: UserRoles): Boolean {
            return getRolesAsEnum().contains(role)
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