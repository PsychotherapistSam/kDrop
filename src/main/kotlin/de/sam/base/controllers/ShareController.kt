package de.sam.base.controllers

import de.sam.base.database.FileDAO
import de.sam.base.database.ShareDAO
import de.sam.base.database.getDAO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.share
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class ShareController {

    fun create(ctx: Context) {
        val file = ctx.formParamAsClass<UUID>("fileId").get()
        val maxDownloads = ctx.formParamAsClass<Long>("maxDownloads").allowNullable().get()
        val vanityName = ctx.formParamAsClass<String>("vanityName").allowNullable().get()
        val password = ctx.formParamAsClass<String>("password").allowNullable().get()

        transaction {
            val fileDAO = FileDAO.findById(file)

            if (fileDAO == null || !fileDAO.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                throw BadRequestResponse("File not found or not owned by you")
            }

            val share = ShareDAO.new {
                this.file = fileDAO
                this.user = ctx.currentUserDTO!!.getDAO()!!
                this.creationDate = DateTime.now()
                this.maxDownloads = maxDownloads
                this.downloadCount = 0
                this.vanityName = vanityName
                this.password = password
            }
            ctx.json(mapOf("id" to share.id.value))
        }
    }

    fun delete(ctx: Context) {
        transaction {
            ctx.share!!.first.delete()
        }
    }

    fun getOne(ctx: Context) {
//        ctx.json(ctx.share!!.second)
    }

    fun update(ctx: Context, resourceId: String) {
        TODO("Not yet implemented")
    }
}