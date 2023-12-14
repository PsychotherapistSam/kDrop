package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.sql.ResultSet
import java.util.*

// The DTO exists to be a serializable object for the session persistence.
//TODO: theoretically doesnt need to be serializable & dto since it is only used in the server and not a session attribute

data class LoginLogDTO(
    var id: UUID,
    @JsonIgnore
    var user: UUID,
    var ip: String,
    var userAgent: String,
    var date: DateTime,
    var sessionId: String? = null,
    var revoked: Boolean = false
) : Serializable

object LoginLogTable : UUIDTable("t_login_log") {
    val user = reference("user", UsersTable)
    val ip = varchar("ip", 255)
    val userAgent = varchar("user_agent", 255)
    val date = datetime("date")
    val sessionId = varchar("session_id", 255).nullable()
    val revoked = bool("revoked").default(false)
}

class LoginLogDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<LoginLogDAO>(LoginLogTable)

    var user by UserDAO referencedOn LoginLogTable.user
    var ip by LoginLogTable.ip
    var userAgent by LoginLogTable.userAgent
    var date by LoginLogTable.date
    var sessionId by LoginLogTable.sessionId
    var revoked by LoginLogTable.revoked
}

fun LoginLogDAO.toDTO(): LoginLogDTO {
    return LoginLogDTO(
        this.id.value,
        this.user.id.value,
        this.ip,
        this.userAgent,
        this.date,
        this.sessionId,
        this.revoked
    )
}

class LoginLogDTOMapper : RowMapper<LoginLogDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): LoginLogDTO {
        return LoginLogDTO(
            id = UUID.fromString(rs.getString("id")),
            user = UUID.fromString(rs.getString("user")),
            ip = rs.getString("ip"),
            userAgent = rs.getString("user_agent"),
            date = DateTime(rs.getTimestamp("date")),
            sessionId = rs.getString("session_id"),
            revoked = rs.getBoolean("revoked")
        )
    }
}