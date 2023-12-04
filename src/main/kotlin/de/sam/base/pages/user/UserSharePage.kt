package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.FileDTO
import de.sam.base.services.FileService
import de.sam.base.services.ShareService
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.share
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserSharePage : Page(
    name = "Shared File",
    templateName = "user/share.kte",
), KoinComponent {
    companion object {
        // never used since it contains a path parameter
        const val ROUTE: String = "/s/"
    }

    private val fileService: FileService by inject()
    private val shareService: ShareService by inject()

    var file: FileDTO? = null
    var fileDTOs = listOf<FileDTO>()

    override fun get() {
        file = fileService.getFileById(ctx.share!!.second.file) ?: throw NotFoundResponse("File not found")
    }

    fun shareList(ctx: Context) {
        val shares = shareService.getSharesForUser(ctx.currentUserDTO!!.id)

        ctx.render(
            "components/files/sharesList.kte",
            mapOf(
                "shares" to shares
            )
        )
    }
}