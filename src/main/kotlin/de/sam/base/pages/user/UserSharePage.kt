package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.authentication.PasswordHasher
import de.sam.base.database.FileDTO
import de.sam.base.database.ShareDTO
import de.sam.base.services.FileService
import de.sam.base.services.ShareService
import de.sam.base.utils.RateLimiter
import de.sam.base.utils.fileDTOFromId
import de.sam.base.utils.realIp
import de.sam.base.utils.share
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import kotlinx.coroutines.runBlocking
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
    private val passwordHasher: PasswordHasher by inject()

    private val rateLimiter: RateLimiter by inject()

    var file: FileDTO? = null
    lateinit var share: ShareDTO

    var providedPassword: String? = null

    var passwordRequired = false
    var passwordCorrect = false // this shows whether the user has provieded the correct password
    var passwordWrong = false // this shows whether the user has tried a wrong password
    var rateLimited = false

    override fun get() {

        share = ctx.share ?: throw NotFoundResponse("Share not found")
        file = fileService.getFileById(share.file) ?: throw NotFoundResponse("File not found")

        passwordRequired = share.password != null

        if (passwordRequired) {
            providedPassword = ctx.queryParam("password")
            if (providedPassword != null) {
                val taken = runBlocking {
                    rateLimiter.share.tryTake(ctx.realIp)
                }

                if (!taken) {
                    rateLimited = true
                }
            }

            passwordCorrect = providedPassword != null && passwordHasher.verifyPassword(
                providedPassword!!, share.password!!, file!!.id.toString()
            ) && !rateLimited

            passwordWrong = providedPassword != null && !passwordCorrect
        }
    }

    fun shareList(ctx: Context) {
        val file = ctx.fileDTOFromId ?: throw NotFoundResponse("File not found")
        val shares = shareService.getSharesForFile(file.id)

        ctx.render(
            "components/files/sharesList.kte", mapOf(
                "shares" to shares
            )
        )
    }
}