package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.joda.time.DateTime
import java.io.Serializable
import java.sql.ResultSet
import java.util.*

// The DTO exists to be a serializable object for the session persistence.

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
    @JsonIgnore
    var password: String?,
) : Serializable

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