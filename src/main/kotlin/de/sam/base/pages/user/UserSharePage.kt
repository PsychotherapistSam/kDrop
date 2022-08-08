package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.controllers.resultFile
import de.sam.base.database.*
import de.sam.base.utils.*
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileInputStream
import kotlin.system.measureNanoTime

class UserSharePage : Page(
    name = "Shared File",
    templateName = "user/share.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var file: FileDTO? = null
    var fileDTOs = listOf<FileDTO>()

    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
//            val shareId = ctx.pathParam("shareId")
            transaction {
//                val share = if (shareId.isUUID) {
//                    ShareDAO.find { SharesTable.id eq UUID.fromString(shareId) }
//                        .limit(1)
//                        .firstOrNull()
//                } else {
//                    ShareDAO.find { SharesTable.vanityName eq shareId }
//                        .limit(1)
//                        .firstOrNull()
//                }
//
                val fileDAO =
                    FileDAO.findById(ctx.share!!.first.file.id) ?: throw NotFoundResponse("File not found")
                file = fileDAO.toDTO()
//                if (file?.isFolder == true) {
//                    logTimeSpent("getting the file list") {
//                        val sortingDirection = FileSortingDirection.sortDirections.first { it.name == "name" }
//
//                        fileDTOs = FileDAO
//                            .find { FilesTable.parent eq fileDAO.id }
//                            .map { it.toDTO() }
//                            .sortedWith { a, b ->
//                                sortingDirection.compare(a, b)
//                            }
//                    }
//                }
            }
        }
        super.handle(ctx)
    }

    fun shareList(ctx: Context) {
        val shares = transaction {
            ShareDAO.find { SharesTable.user eq ctx.currentUserDTO!!.id and (SharesTable.file eq ctx.fileDTOFromId!!.id) }
                .toList()
        }
        ctx.render(
            "components/files/sharesList.kte",
            mapOf(
                "shares" to shares
            )
        )
    }

    fun downloadFile(ctx: Context) {
        //TODO: this using a context extension
        val file = transaction {
            FileDAO.findById(ctx.share!!.first.file.id)?.toDTO()
                ?: throw NotFoundResponse("File not found").also { Logger.error(it.message) }
        }

        val systemFile = File("./${file.path}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found").also { Logger.error(it.message) }
        }
        val isDirectDownload = ctx.queryParam("download") != null

        // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
        val dispositionType = if (isDirectDownload) "attachment" else "inline"

        ctx.header("Cache-Control", "max-age=31536000, immutable")

        if (ctx.header(Header.RANGE) == null) {
            ctx.resultFile(systemFile, file.name, file.mimeType, dispositionType)

//            ctx.header(Header.CONTENT_TYPE, file.mimeType)
//            ctx.header(Header.CONTENT_DISPOSITION, "$dispositionType; filename=${file.name}")
//            ctx.header(Header.CONTENT_LENGTH, file.size.toString())
//            ctx.result(FileInputStream(systemFile))

//            if (isDirectDownload) {
//                transaction {
//                    logTimeSpent("adding file log entry") {
//                        DownloadLogDAO.new {
//                            this.file = ctx.fileDAOFromId
//                            this.user = ctx.currentUserDTO?.getDAO()
//                            this.ip = ctx.ip()
//                            this.readDuration = System.nanoTime() - ctx.requestStartTime
//                            this.downloadDate = DateTime.now() - (this.readDuration / 1000000L)
//                            this.readBytes = file.size
//                            this.userAgent = ctx.header(Header.USER_AGENT) ?: "unknown"
//                        }
//                    }
//                }
//            }
        } else {
            CustomSeekableWriter.write(ctx, FileInputStream(systemFile), file.mimeType, file.size)
        }
    }
}


/*
private fun File.toKFile(): KFile {
    return KFile(
        id = UUID.randomUUID(),
        name = this.name,
        parent = this.parentFile.let { it?.name },
        size = "ooga GB",
        lastModified = "now",
        isDirectory = this.isDirectory,
        children = this.listFiles().let { files -> files.orEmpty().map { b -> b.name } }
    )
}
*/