package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.controllers.resultFile
import de.sam.base.database.*
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.fileDAOFromId
import de.sam.base.utils.fileDTOFromId
import de.sam.base.utils.share
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.tinylog.kotlin.Logger
import java.io.File

class UserSharePage : Page(
    name = "Shared File",
    templateName = "user/share.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var file: FileDTO? = null
    var fileDTOs = listOf<FileDTO>()

    override fun get() {
        transaction {
            val fileDAO =
                FileDAO.findById(ctx.share!!.first.file.id) ?: throw NotFoundResponse("File not found")
            file = fileDAO.toDTO()

            if (file?.isFolder == true) {
                fileDTOs = FileDAO
                    .find { FilesTable.parent.eq(ctx.fileDAOFromId?.id) }
                    .map { it.toDTO() }
//                    .sortedWith { a, b ->
//                        sortingDirection.compare(a, b)
//                    }
            }
        }
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

        ctx.resultFile(systemFile, file.name, file.mimeType!!, dispositionType)
    }
}