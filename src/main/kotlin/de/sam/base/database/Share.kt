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

class ShareDTO(
    var id: UUID,
    @JsonIgnore
    var file: UUID,
    @JsonIgnore
    var user: UUID,
    var creationDate: DateTime,
    var maxDownloads: Long?,
    var downloadCount: Long,
    var vanityName: String?,
    var password: String?,
) : Serializable

object SharesTable : UUIDTable("t_shares") {
    val file = reference("file", FilesTable)
    val user = reference("user", UsersTable)
    val creationDate = datetime("creation_date")
    val maxDownloads = long("max_downloads").nullable()
    val downloadCount = long("download_count")
    val vanityName = varchar("vanity_name", 255).nullable()
    val password = varchar("password", 255).nullable()
}

class ShareDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<ShareDAO>(SharesTable)

    var file by FileDAO referencedOn SharesTable.file
    var user by UserDAO referencedOn SharesTable.user
    var creationDate by SharesTable.creationDate
    var maxDownloads by SharesTable.maxDownloads
    var downloadCount by SharesTable.downloadCount
    var vanityName by SharesTable.vanityName
    var password by SharesTable.password
}

fun ShareDAO.toDTO(): ShareDTO {
    return ShareDTO(
        this.id.value,
        this.file.id.value,
        this.user.id.value,
        this.creationDate,
        this.maxDownloads,
        this.downloadCount,
        this.vanityName,
        this.password
    )
}

class ShareDTOMapper : RowMapper<ShareDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): ShareDTO {
        return ShareDTO(
            id = UUID.fromString(rs.getString("id")),
            file = UUID.fromString(rs.getString("file")),
            creationDate = DateTime(rs.getTimestamp("creation_date")),
            maxDownloads = rs.getLong("max_downloads"),
            downloadCount = rs.getLong("download_count"),
            vanityName = rs.getString("vanity_name"),
            password = rs.getString("password"),
            user = UUID.fromString(rs.getString("user"))
        )
    }
}