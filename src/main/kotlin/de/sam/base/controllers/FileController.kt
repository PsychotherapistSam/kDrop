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

        val parentId = if (ctx.queryParam("parent") != null) UUID.fromString(ctx.queryParam("parent")) else null

        val files = ctx.uploadedFiles()
        transaction {
            val owner = UserDAO.find { UsersTable.id eq ctx.currentUserDTO!!.id }.first()
            val parent = if (parentId != null) FileDAO.findById(parentId) else null

            val idMap = mutableMapOf<String, UUID>()

            files.forEach {
                val uploadFolder = File("./upload/")
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdir()
                }

                val file = FileDAO.new {
                    this.name = it.filename
                    this.path = "upload/${this.id}"
                    this.mimeType = it.contentType ?: "application/octet-stream"
                    this.parent = parent
                    this.owner = owner
                    this.size = it.size
                    this.sizeHR = humanReadableByteCountBin(it.size)
                    this.password = null
                    this.private = true
                    this.created = DateTime.now()
                    this.isFolder = false
                }

                FileUtil.streamToFile(it.content, "./upload/${file.id}")

                idMap[file.name] = file.id.value
            }
            ctx.json(idMap)
        }
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
                // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
                val dispositionType =
                    if (ctx.queryParam("download") == null) "inline" else "attachment"

                ctx.header("Content-Type", file.mimeType)
                ctx.header("Content-Disposition", "${dispositionType}; filename=${file.name}")
                ctx.header("Content-Length", file.size.toString())

                ctx.result(FileInputStream(systemFile))
            } else {
                throw NotFoundResponse("File not found")
            }
        } else {
            throw NotFoundResponse("File not found")
        }
    }

    fun deleteSingleFile(ctx: Context) {
        val file = ctx.attribute<FileDTO>("requestFileParameter")
        if (file == null || file.owner.id != ctx.currentUserDTO!!.id) {
            throw NotFoundResponse("File not found")
        }

        val systemFile = File("./${file.path}")
        if (systemFile.exists()) {
            systemFile.delete()
        }
        if (!systemFile.exists()) {
            transaction {
                FileDAO.findById(file.id)!!.delete()
            }
        }

        //TODO: if file is a folder, delete all files in it

        ctx.json(mapOf("status" to "ok"))
    }

    fun deleteFiles(ctx: Context) {
        val fileListString = ctx.formParamAsClass<String>("files")
            .check({ files ->
                files
                    .split(",")
                    .all { file ->
                        file.isValidUUID()
                    }
            }, "Invalid UUID")
            .get()

        val fileIDs = fileListString.split(",").map { UUID.fromString(it) }
        val deletedFileIDs = arrayListOf<UUID>()
        transaction {
            FileDAO.find { FilesTable.id inList fileIDs }.forEach { file ->
                if (file.owner.id.value != ctx.currentUserDTO!!.id) {
                    return@forEach
                }

                val systemFile = File("./${file.path}")
                if (systemFile.exists()) {
                    systemFile.delete()
                }
                if (!systemFile.exists()) {
                    file.delete()
                }
                deletedFileIDs.add(file.id.value)
            }
        }

        val filesNotDeleted = fileIDs.filter { !deletedFileIDs.contains(it) }
        if (filesNotDeleted.isNotEmpty()) {
            ctx.json(mapOf("status" to "error", "filesNotDeleted" to filesNotDeleted, "filesDeleted" to fileIDs))
        } else {
            ctx.json(mapOf("status" to "ok", "filesDeleted" to fileIDs))
        }
    }

    fun createDirectory(ctx: Context) {
        val folderName = ctx.formParamAsClass<String>("name").get()

        val parentId = if (ctx.queryParam("parent") != null) UUID.fromString(ctx.queryParam("parent")) else null

        transaction {
            val owner = UserDAO.find { UsersTable.id eq ctx.currentUserDTO!!.id }.first()
            val parent = if (parentId != null) FileDAO.findById(parentId) else null

            val file = FileDAO.new {
                this.name = folderName
                this.path = "upload/${this.id}"
                this.mimeType = ""
                this.parent = parent
                this.owner = owner
                this.size = 0
                this.sizeHR = "0 B"
                this.password = null
                this.private = true
                this.created = DateTime.now()
                this.isFolder = true
            }
            ctx.json(mapOf("id" to file.id.toString()))
        }
    }


    fun deleteDirectory(ctx: Context) {

    }
}

private fun String.isValidUUID(): Boolean {
    try {
        UUID.fromString(this)
    } catch (exception: IllegalArgumentException) {
        return false
    }
    return true
}
