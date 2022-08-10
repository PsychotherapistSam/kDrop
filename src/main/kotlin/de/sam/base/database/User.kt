package de.sam.base.database

import de.sam.base.users.UserRoles
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

// The UserDTO exists to be a serializable object for the session persistance.
class UserDTO(
    var id: UUID,
    var name: String,
    var password: String,
    var roles: List<UserRoles>,
    var preferences: String,
    var registrationDate: DateTime,
    var totpSecret: String?,
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

//    fun getPreference(preference: String): String? {
//        val prefs = preferences.split(",")
//        val index = prefs.indexOf(preference)
//        if (index == -1) {
//            return null
//        }
//        return prefs[index + 1]
//    }
}
// Extract this from the serializable object.
fun UserDTO.fetchDAO(): UserDAO? {
    return transaction {
        UserDAO.findById(this@fetchDAO.id)
    }
}

object UsersTable : UUIDTable("t_users") {
    val name = varchar("name", 50)
    val password = varchar("password", 256)
    val roles = varchar("roles", 50)
    val hidden = bool("hidden")
    val preferences = varchar("preferences", 256)
    val registrationDate = datetime("registration_date")
    var totpSecret = varchar("totp_secret", 256).nullable()
}

class UserDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDAO>(UsersTable)

    var name by UsersTable.name
    var password by UsersTable.password
    var roles by UsersTable.roles
    var hidden by UsersTable.hidden
    var preferences by UsersTable.preferences
    var registrationDate by UsersTable.registrationDate
    var totpSecret by UsersTable.totpSecret
}


fun UserDAO.toDTO(): UserDTO {
    return UserDTO(
        this.id.value,
        this.name,
        this.password,
        roles.split(",").map { UserRoles.valueOf(it) },
        this.preferences,
        this.registrationDate,
        this.totpSecret
    )
}