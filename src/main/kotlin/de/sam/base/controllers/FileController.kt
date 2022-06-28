package de.sam.base.controllers

import com.password4j.Argon2Function
import com.password4j.types.Argon2
import de.sam.base.database.*
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.humanReadableByteCountBin
import io.javalin.core.util.FileUtil
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.system.measureNanoTime

class FileController {
    private val argon2Instance = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)

    fun uploadFile(ctx: Context) {
        val maxFileSize = 1024L * 1024L * 1024L * 5L // 1024 MB || 5 GiB
        if (ctx.header("Content-Length") != null && ctx.header("Content-Length")!!.toLong() > maxFileSize) {
            throw BadRequestResponse("File too big, max size is ${humanReadableByteCountBin(maxFileSize)}")
        }

        val files = ctx.uploadedFiles()
        transaction {
            val owner = UserDAO.find { UsersTable.id eq ctx.currentUserDTO!!.id }.first()

            files.forEach {
                val uploadFolder = File("./upload/")
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdir()
                }

                val file = FileDAO.new {
                    this.name = it.filename
                    this.path = "upload/${this.id}"
                    this.parent = null
                    this.owner = owner
                    this.size = it.size
                    this.sizeHR = humanReadableByteCountBin(it.size)
                    this.password = null
                    this.private = false
                    this.created = DateTime.now()
                    this.isFolder = false
                }

                FileUtil.streamToFile(it.content, "./upload/${file.id}")
            }
        }

        ctx.json(mapOf("status" to "ok"))
    }

    fun getFileParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("fileId")
                .check({
                    transaction {
                        logTimeSpent("Getting file by id") {
                            val fileDao = FileDAO.findById(it)
                            if (fileDao != null) {
                                ctx.attribute("requestFileParameter", fileDao.toFile())
                                return@transaction true
                            } else {
                                return@transaction false
                            }
                        }
                    }
                }, "File ID is not valid")
                .get()
        }
        ctx.attribute("fileQueryTime", userQueryTime)
    }

    fun getFile(ctx: Context) {
        val file = ctx.attribute<FileDTO>("requestFileParameter")
        if (file != null) {
            val systemFile = File("./${file.path}")
            if (systemFile.exists()) {
                ctx.header("Content-Type", "application/octet-stream")
                ctx.header("Content-Disposition", "attachment; filename=${file.name}")
                ctx.result(FileInputStream(systemFile))
            } else {
                throw NotFoundResponse("File not found")
            }
        } else {
            throw NotFoundResponse("File not found")
        }
    }

    fun deleteFile(ctx: Context) {
    }
}