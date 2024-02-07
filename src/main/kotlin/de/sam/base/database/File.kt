package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import de.sam.base.file.FileType
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.joda.time.DateTime
import java.io.Serializable
import java.sql.ResultSet
import java.util.*

//TODO: theoretically doesnt need to be serializable & dto since it is only used in the server and not a session attribute

data class FileDTO(
    var id: UUID,
    var name: String,
    var path: String? = null,
    var mimeType: String? = null,
    @JsonIgnore
    var parent: UUID? = null,
    @JsonIgnore
    var owner: UUID? = null,
    var size: Long? = null,
    var sizeHR: String? = null,
    @JsonIgnore
    var password: String? = null,
    var created: DateTime? = null,
    var isFolder: Boolean,
    var hash: String? = null,
    var isRoot: Boolean? = null,
) : Serializable {
    fun isOwnedByUserId(id: UUID?): Boolean {
        return id != null && owner == id
    }

    fun getTypeEnum(): FileType {
        return FileType.fromMimeType(mimeType!!)
    }
}

class FileDTOMapper : RowMapper<FileDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): FileDTO {
        return FileDTO(
            id = UUID.fromString(rs.getString("id")),
            name = rs.getString("name"),
            path = rs.getString("path"),
            mimeType = rs.getString("mime_type"),
            parent = rs.getString("parent")?.let { UUID.fromString(it) },
            owner = UUID.fromString(rs.getString("owner")),
            size = rs.getLong("size"),
            sizeHR = rs.getString("size_hr"),
            password = rs.getString("password"),
            created = DateTime(rs.getTimestamp("created")),
            isFolder = rs.getBoolean("is_folder"),
            hash = rs.getString("hash"),
            isRoot = rs.getBoolean("is_root")
        )
    }
}
