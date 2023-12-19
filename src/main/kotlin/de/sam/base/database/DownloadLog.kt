package de.sam.base.database

import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

// The DTO exists to be a serializable object for the session persistence.
//TODO: theoretically doesnt need to be serializable & dto since it is only used in the server and not a session attribute

class DownloadLogDTO(
    var id: UUID,
    var file: FileDTO?,
    var user: UserDTO?,
    var ip: String,
    var downloadDate: DateTime,
    var readBytes: Long,
    var readDuration: Long,
    var userAgent: String,
    var zipFileName: String?,
) : Serializable
