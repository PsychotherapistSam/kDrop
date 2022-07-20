package de.sam.base.database

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

// The DTO exists to be a serializable object for the session persistence.
class DownloadLogDTO(
    var id: UUID,
    var file: FileDTO,
    var user: UserDTO?,
    var ip: String,
    var downloadDate: DateTime,
    var readBytes: Long,
    var readDuration: Long,
    var userAgent: String,
) : Serializable

object DownloadLogTable : UUIDTable("t_download_log") {
    val file = reference("file", FilesTable)
    val user = reference("user",UsersTable).nullable()
    val ip = varchar("ip", 50)
    val downloadDate = datetime("download_date")
    val readBytes = long("read_bytes")
    val readDuration = long("read_duration")
    val userAgent = varchar("user_agent", 500)
}

class DownloadLogDAO(id: EntityID<UUID>) : Serializable, UUIDEntity(id) {
    companion object : UUIDEntityClass<DownloadLogDAO>(DownloadLogTable)

    var file by FileDAO referencedOn DownloadLogTable.file
    var user by UserDAO optionalReferencedOn DownloadLogTable.user
    var ip by DownloadLogTable.ip
    var downloadDate by DownloadLogTable.downloadDate
    var readBytes by DownloadLogTable.readBytes
    var readDuration by DownloadLogTable.readDuration
    var userAgent by DownloadLogTable.userAgent
}


fun DownloadLogDAO.toDTO(): DownloadLogDTO {
    return DownloadLogDTO(
        this.id.value,
        this.file.toDTO(),
        this.user?.toDTO(),
        this.ip,
        this.downloadDate,
        this.readBytes,
        this.readDuration,
        this.userAgent
    )
}