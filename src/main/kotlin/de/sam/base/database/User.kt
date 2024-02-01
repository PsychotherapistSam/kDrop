package de.sam.base.database

import de.sam.base.users.UserRoles
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.joda.time.DateTime
import java.io.Serializable
import java.sql.ResultSet
import java.util.*

// The UserDTO exists to be a serializable object for the session persistance.
data class UserDTO(
    var id: UUID,
    var name: String,
    var password: String,
    var roles: List<UserRoles>,
    var preferences: String,
    var registrationDate: DateTime,
    var totpSecret: String?,
    var rootFolderId: UUID?,
    var salt: String?,
    var lastLogin: DateTime? = null
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
            salt = rs.getString("salt"),
            lastLogin = DateTime(rs.getTimestamp("last_login"))
        )
    }
}
