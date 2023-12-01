package de.sam.base.database

import com.fasterxml.jackson.annotation.JsonIgnore
import de.sam.base.utils.file.FileType
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

    // placeholder for functions
}


object FilesTable : UUIDTable("t_files") {
    val name = varchar("name", 300)
    val path = varchar("path", 128)
    val mimeType = varchar("mime_type", 128)
    val parent = reference("parent", FilesTable).nullable()
    val owner = reference("owner", UsersTable)
    val size = long("size")
    val sizeHR = varchar("size_hr", 128)
    val password = varchar("password", 256).nullable()
    val created = datetime("created")
    val isFolder = bool("is_folder")
    val hash = varchar("hash", 128).nullable()
    val isRoot = bool("is_root")
}

class FileDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    fun canBeViewedByUserId(id: UUID): Boolean {
        return owner.id.value == id
    }

    fun isOwnedByUserId(id: UUID): Boolean {
        return owner.id.value == id
    }

    companion object : UUIDEntityClass<FileDAO>(FilesTable)

    var name by FilesTable.name
    var path by FilesTable.path
    var mimeType by FilesTable.mimeType
    var parent by FileDAO optionalReferencedOn FilesTable.parent
    var owner by UserDAO referencedOn FilesTable.owner
    var size by FilesTable.size
    var sizeHR by FilesTable.sizeHR
    var password by FilesTable.password
    var created by FilesTable.created
    var isFolder by FilesTable.isFolder
    var hash by FilesTable.hash
    var isRoot by FilesTable.isRoot
}


fun FileDAO.toDTO(): FileDTO {
    return FileDTO(
        this.id.value,
        this.name,
        this.path,
        this.mimeType,
        this.parent?.id?.value,
        this.owner.id.value,
        this.size,
        this.sizeHR,
        this.password,
        this.created,
        this.isFolder,
        this.hash,
        this.isRoot
    )
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
