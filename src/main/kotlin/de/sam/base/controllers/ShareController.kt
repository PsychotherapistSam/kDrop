package de.sam.base.controllers

import de.sam.base.database.FileDAO
import de.sam.base.database.ShareDAO
import de.sam.base.database.SharesTable
import de.sam.base.database.fetchDAO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.share
import de.sam.base.utils.string.isUUID
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class ShareController {

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

        val password = ctx.formParamAsClass<String>("password").allowNullable()

        val errors = fileId.errors() + maxDownloads.errors() + vanityName.errors() + password.errors()
        if (errors.isNotEmpty()) {
            throw BadRequestResponse(errors.map { it.value }[0][0].message)
        }

        val cleanedName = if (vanityName.get().isNullOrBlank()) {
            null
        } else {
            vanityName.get()
        }

        transaction {
            if (cleanedName != null && ShareDAO.find { SharesTable.vanityName eq cleanedName }.firstOrNull() != null) {
                throw BadRequestResponse("Could not create share (name already exists or is forbidden)")
            }
            val fileDAO = FileDAO.findById(fileId.get())

            if (fileDAO == null || !fileDAO.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                throw BadRequestResponse("File not found or not owned by you")
            }
            val share = ShareDAO.new {
                this.file = fileDAO
                this.user = ctx.currentUserDTO!!.fetchDAO()!!
                this.creationDate = DateTime.now()
                this.maxDownloads = maxDownloads.get()
                this.downloadCount = 0
                this.vanityName = cleanedName
                this.password = password.get()
            }
            ctx.json(mapOf("id" to share.id.value))
        }
    }

//    private fun <T> transactionCommitAndCatch(statement: Transaction.() -> T) = transaction {
//        try {
//            statement()
//            commit()
//        } catch (e: ExposedSQLException) {
//            Logger.warn(e.message)
//            Logger.warn("A user tried to create a share with a name that already exists")
//            throw BadRequestResponse("Could not create share (name already exists or is forbidden)")
//        }
//    }

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