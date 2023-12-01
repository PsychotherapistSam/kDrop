package de.sam.base.database

import de.sam.base.users.UserRoles
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.Serializable
import java.sql.ResultSet
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
    var rootFolderId: UUID?,
    var salt: String?
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
    val preferences = varchar("preferences", 256)
    val registrationDate = datetime("registration_date")
    var totpSecret = varchar("totp_secret", 256).nullable()
    val rootFolderId = uuid("root_folder_id").nullable()
    val salt = varchar("salt", 64).nullable()
}

class UserDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDAO>(UsersTable)

    var name by UsersTable.name
    var password by UsersTable.password
    var roles by UsersTable.roles
    var preferences by UsersTable.preferences
    var registrationDate by UsersTable.registrationDate
    var totpSecret by UsersTable.totpSecret
    var rootFolderId by UsersTable.rootFolderId
    var salt by UsersTable.salt
}


fun UserDAO.toDTO(): UserDTO {
    return UserDTO(
        this.id.value,
        this.name,
        this.password,
        roles.split(",").map { UserRoles.valueOf(it) },
        this.preferences,
        this.registrationDate,
        this.totpSecret,
        this.rootFolderId,
        this.salt
    )
}

class UserDTOMapper : RowMapper<UserDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): UserDTO {
        return UserDTO(
            id = UUID.fromString(rs.getString("id")),
            name = rs.getString("name"),
            password = rs.getString("password"),
            roles = rs.getString("roles").split(",").map { UserRoles.valueOf(it) },
            preferences = rs.getString("preferences"),
            registrationDate = DateTime(rs.getTimestamp("registration_date")),
            totpSecret = rs.getString("totp_secret"),
            rootFolderId = rs.getString("root_folder_id")?.let { UUID.fromString(it) },
            salt = rs.getString("salt")
        )
    }
}
