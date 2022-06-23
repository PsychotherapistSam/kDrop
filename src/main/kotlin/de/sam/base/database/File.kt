package de.sam.base.database

import de.sam.base.users.UserRoles
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

class File(
    var id: UUID,
    var name: String,
    var path: String,
    var parent: File?,
    var owner: User,
    var size: Long,
    var password: String?,
    var private: Boolean,
    var created: DateTime,
    var isFolder: Boolean,
) : Serializable {
    // placeholder for functions
}


object FilesTable : UUIDTable("t_files") {
    val name = varchar("name", 128)
    val path = varchar("path", 128)
    val parent = reference("parent", FilesTable).nullable()
    val owner = reference("owner", UsersTable)
    val size = long("size")
    val password = varchar("password", 256).nullable()
    val private = bool("private")
    val created = datetime("created")
    val isFolder = bool("is_folder")

}

class FileDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<FileDAO>(FilesTable)

    var name by FilesTable.name
    var path by FilesTable.path
    var parent by FileDAO optionalReferencedOn FilesTable.parent
    var owner by UserDAO referencedOn FilesTable.owner
    var size by FilesTable.size
    var password by FilesTable.password
    var private by FilesTable.private
    var created by FilesTable.created
    var isFolder by FilesTable.isFolder
}


fun FileDAO.toFile(): File {
    return File(
        this.id.value,
        this.name,
        this.path,
        this.parent?.toFile(),
        this.owner.toUser(),
        this.size,
        this.password,
        this.private,
        this.created,
        this.isFolder
    )
}