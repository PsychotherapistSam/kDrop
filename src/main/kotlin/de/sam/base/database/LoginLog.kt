package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

// The DTO exists to be a serializable object for the session persistence.
//TODO: theoretically doesnt need to be serializable & dto since it is only used in the server and not a session attribute

class LoginLogDTO(
    var id: UUID,
    @JsonIgnore
    var user: UserDTO,
    var ip: String,
    var userAgent: String,
    var date: DateTime,
) : Serializable

object LoginLogTable : UUIDTable("t_login_log") {
    val user = reference("user", UsersTable)
    val ip = varchar("ip", 255)
    val userAgent = varchar("user_agent", 255)
    val date = datetime("date")
}

class LoginLogDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<LoginLogDAO>(LoginLogTable)

    var user by UserDAO referencedOn LoginLogTable.user
    var ip by LoginLogTable.ip
    var userAgent by LoginLogTable.userAgent
    var date by LoginLogTable.date
}

fun LoginLogDAO.toDTO(): LoginLogDTO {
    return LoginLogDTO(
        this.id.value,
        this.user.toDTO(),
        this.ip,
        this.userAgent,
        this.date
    )
}