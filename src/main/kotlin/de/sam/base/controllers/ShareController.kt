package de.sam.base.controllers

import de.sam.base.authentication.PasswordHasher
import de.sam.base.database.ShareDTO
import de.sam.base.services.FileService
import de.sam.base.services.ShareService
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.share
import de.sam.base.utils.string.isUUID
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.formParamAsClass
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class ShareController : KoinComponent {

    private val shareService: ShareService by inject()
    private val fileService: FileService by inject()
    private val passwordHasher: PasswordHasher by inject()

    fun create(ctx: Context) {
        val fileId = ctx.formParamAsClass<UUID>("fileId")
        val maxDownloads = ctx.formParamAsClass<Long>("maxDownloads").allowNullable()
        val vanityName = ctx.formParamAsClass<String>("vanityName").allowNullable()
            .check({ it == null || it.length <= 32 }, "Vanity is too long")
            .check({ it == null || !it.isUUID }, "Vanity can not be a UUID")
            .check(
                { it == null || !"[^a-z\\d.]".toRegex().containsMatchIn(it) },
                "Could not create share (name already exists or is forbidden)"
            )

        val passwordValidator = ctx.formParamAsClass<String>("password").allowNullable()

        val errors = fileId.errors() + maxDownloads.errors() + vanityName.errors() + passwordValidator.errors()
        if (errors.isNotEmpty()) {
            throw BadRequestResponse(errors.map { it.value }[0][0].message)
        }

        val cleanedName = if (vanityName.get().isNullOrBlank()) {
            null
        } else {
            vanityName.get()
        }

        if (cleanedName != null && shareService.getShareByName(cleanedName) != null) {
            throw BadRequestResponse("Could not create share (name already exists or is forbidden)")
        }

        val file = fileService.getFileById(fileId.get())

        if (file == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
            throw BadRequestResponse("File not found or not owned by you")
        }

        val unhashedPassword = passwordValidator.get()
        val hashedPassword =
            if (unhashedPassword.isNullOrBlank()) {
                null
            } else {
                passwordHasher.hashPassword(unhashedPassword, file.id.toString())
            }


        val newShare = shareService.createShare(
            ShareDTO(
                UUID.randomUUID(),
                file.id,
                ctx.currentUserDTO!!.id,
                DateTime.now(),
                maxDownloads.get(),
                0,
                cleanedName,
                hashedPassword
            )
        )
        ctx.json(mapOf("id" to newShare.id))
    }

    fun delete(ctx: Context) {
        shareService.deleteShare(ctx.share!!.second.id)
    }

    fun getOne(ctx: Context) {
//        ctx.json(ctx.share!!.second)
    }

    fun update(ctx: Context, resourceId: String) {
        TODO("Not yet implemented")
    }
}