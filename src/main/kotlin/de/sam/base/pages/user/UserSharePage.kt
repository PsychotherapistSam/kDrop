package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.authentication.PasswordHasher
import de.sam.base.database.FileDTO
import de.sam.base.database.ShareDTO
import de.sam.base.file.repository.FileRepository
import de.sam.base.file.share.ShareRepository
import de.sam.base.utils.*
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

    private val fileRepository: FileRepository by inject()
    private val shareRepository: ShareRepository by inject()
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
        file = fileRepository.fileCache.get(share.file) ?: throw NotFoundResponse("File not found")

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
        val file = fileRepository.fileCache.get(ctx.fileId) ?: throw NotFoundResponse("File not found")
        val shares = shareRepository.getSharesForFile(file.id)

        ctx.render(
            "components/files/sharesList.kte", mapOf(
                "shares" to shares,
                "modal" to true
            )
        )
    }
}