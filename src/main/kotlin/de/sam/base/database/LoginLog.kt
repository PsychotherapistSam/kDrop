package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
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
    var revoked: Boolean = false,
    var failed: Boolean = false
) : Serializable

class LoginLogDTOMapper : RowMapper<LoginLogDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): LoginLogDTO {
        return LoginLogDTO(
            id = UUID.fromString(rs.getString("id")),
            user = UUID.fromString(rs.getString("user")),
            ip = rs.getString("ip"),
            userAgent = rs.getString("user_agent"),
            date = DateTime(rs.getTimestamp("date")),
            sessionId = rs.getString("session_id"),
            revoked = rs.getBoolean("revoked"),
            failed = rs.getBoolean("failed")
        )
    }
}