package de.sam.base.database

import de.sam.base.users.UserRoles
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

class User(
    var id: UUID,
    var name: String,
    var password: String,
    var roles: List<UserRoles>,
    var preferences: String,
    var registrationDate: DateTime
) : Serializable {

    fun getHighestRolePowerLevel(): Int = roles.maxOf { it.powerLevel }

    fun getHighestRole(): UserRoles {
        return roles.maxByOrNull { it.powerLevel }!! // users should not not have a role
    }

    fun hasRolePowerLevel(role: UserRoles): Boolean {
        return role.powerLevel <= getHighestRolePowerLevel()
    }

    fun hasRole(role: UserRoles): Boolean {
        return roles.contains(role)
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


object UsersTable : UUIDTable("t_users") {
    val name = varchar("name", 50)
    val password = varchar("password", 256)
    val roles = varchar("roles", 50)
    val hidden = bool("hidden")
    val preferences = varchar("preferences", 256)
    val registrationDate = datetime("registration_date")
}

class UserDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDAO>(UsersTable)

    var name by UsersTable.name
    var password by UsersTable.password
    var roles by UsersTable.roles
    var hidden by UsersTable.hidden
    var preferences by UsersTable.preferences
    var registrationDate by UsersTable.registrationDate
}


fun UserDAO.toUser(): User {
    return User(
        this.id.value,
        this.name,
        this.password,
        roles.split(",").map { UserRoles.valueOf(it) },
        this.preferences,
        this.registrationDate
    )
}