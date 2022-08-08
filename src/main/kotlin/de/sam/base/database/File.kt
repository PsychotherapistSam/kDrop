package de.sam.base.database

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

//TODO: theoretically doesnt need to be serializable & dto since it is only used in the server and not a session attribute

class FileDTO(
    var id: UUID,
    var name: String,
    var path: String,
    var mimeType: String,
    var parent: FileDTO?,
    var owner: UserDTO,
    var size: Long,
    var sizeHR: String,
    var password: String?,
    var created: DateTime,
    var isFolder: Boolean,
    var hash: String?
) : Serializable {
    fun isOwnedByUserId(id: UUID?): Boolean {
        return id != null && owner.id == id
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
    val private = bool("private")
    val created = datetime("created")
    val isFolder = bool("is_folder")
    val hash = varchar("hash", 128).nullable()
}

class FileDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    fun canBeViewedByUserId(id: UUID): Boolean {
        return !private || owner.id.value == id
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
    var private by FilesTable.private
    var created by FilesTable.created
    var isFolder by FilesTable.isFolder
    var hash by FilesTable.hash
}


fun FileDAO.toDTO(): FileDTO {
    return FileDTO(
        this.id.value,
        this.name,
        this.path,
        this.mimeType,
        this.parent?.toDTO(),
        this.owner.toDTO(),
        this.size,
        this.sizeHR,
        this.password,
        this.created,
        this.isFolder,
        this.hash
    )
}