package de.sam.base.controllers

import de.sam.base.database.*
import de.sam.base.utils.CustomSeekableWriter
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.file.zipFiles
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
import java.lang.Thread.sleep
import java.security.MessageDigest
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime

class FileController {
    fun uploadFile(ctx: Context) {
        val maxFileSize = 1024L * 1024L * 1024L * 5L // 1024 MB || 5 GiB
        if (ctx.header("Content-Length") != null && ctx.header("Content-Length")!!.toLong() > maxFileSize) {
            throw BadRequestResponse("File too big, max size is ${humanReadableByteCountBin(maxFileSize)}")
        }

        val parentId = if (ctx.queryParam("parent") != null) UUID.fromString(ctx.queryParam("parent")) else null


        val files = ctx.uploadedFiles()
        transaction {
            val owner = UserDAO.findById(ctx.currentUserDTO!!.id)!!
            val parent = if (parentId != null) FileDAO.findById(parentId) else null

            if (parent != null && !parent.toFileDTO().isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                throw BadRequestResponse("Parent folder does not exist or is not owned by you")
            }

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

                val targetFile = File("./upload/${file.id}")
                FileUtil.streamToFile(it.content, targetFile.path)

                logTimeSpent("hashing file") {
                    file.hash = targetFile.sha512()
                }

                idMap[file.name] = file.id.value
            }
            ctx.json(idMap)
        }
    }

    private val cache = mutableMapOf<UUID, Pair<Long, FileDTO>>()
    fun getFileParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("fileId")
                .check({
                    if (cache.containsKey(it)) {
                        if (System.currentTimeMillis() < cache[it]!!.first + 1000 * 60) {
                            ctx.attribute("requestFileParameter", cache[it]!!.second)
                            return@check true
                        } else {
                            cache.remove(it)
                        }
                    }
                    transaction {
                        logTimeSpent("Getting file by id") {
                            val fileDao = FileDAO.findById(it)
                            if (fileDao != null) {
                                ctx.attribute("requestFileParameter", fileDao.toFileDTO())
                                cache[it] = Pair(System.currentTimeMillis(), fileDao.toFileDTO())
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

    fun getSingleFile(ctx: Context) {
        val file = ctx.attribute<FileDTO>("requestFileParameter") ?: throw NotFoundResponse("File not found")

        // the file is private and the user isn't logged in or the file isn't owned by the user
        if (file.private && (ctx.currentUserDTO == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id))) {
            throw NotFoundResponse("File not found")
        }

        val systemFile = File("./${file.path}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found")
        }
        // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
        val dispositionType =
            if (ctx.queryParam("download") == null) "inline" else "attachment"

        ctx.header("Content-Type", file.mimeType)
        ctx.header("Content-Disposition", "${dispositionType}; filename=${file.name}")
        ctx.header("Content-Length", file.size.toString())

        CustomSeekableWriter.write(ctx, FileInputStream(systemFile), file.mimeType, file.size)
        // ctx.seekableStream(FileInputStream(systemFile), file.mimeType, file.size)
    }

    fun getFiles(ctx: Context) {
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

        val fileList = arrayListOf<Pair<File, String>>()
        transaction {
            FileDAO.find { FilesTable.id inList fileIDs }.forEach { file ->
                if (!file.toFileDTO().canBeViewedByUserId(ctx.currentUserDTO!!.id)) {
                    return@forEach
                }

                if (file.isFolder) {
                    // add all files and subfolders recursively
                    fileList.addAll(getChildren(file, ctx.currentUserDTO!!, file.name + "/"))
                } else {
                    val systemFile = File("./${file.path}")
                    if (systemFile.exists()) {
                        fileList.add(Pair(systemFile, file.name))
                    }
                }
            }
        }

        val tempZipFile = File("./upload/temp/${UUID.randomUUID()}.zip")

        val nanoTime = measureNanoTime {
            zipFiles(fileList, tempZipFile)
        }

        val milliTime = nanoTime / 1000000.0
        println("Zipping took $milliTime ms")

        println("Zipping done")

        if (tempZipFile.exists()) {
            // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph

            ctx.header("Content-Type", "application/zip")
            ctx.header(
                "Content-Disposition",
                "attachment; filename=download_${DateTime.now().toString("yyyy-MM-dd_HH-mm-ss")}.zip"
            )
            ctx.header("Content-Length", tempZipFile.length().toString())

            ctx.result(FileInputStream(tempZipFile))
        } else {
            throw NotFoundResponse("File not found")
        }

        thread {
            var counter = 0
            while (tempZipFile.exists()) {
                tempZipFile.delete()
                counter++
                sleep(1000)
                // if file can not be deleted at runtime, it will be deleted on the next stop
                if (counter > 60) {
                    tempZipFile.deleteOnExit()
                    return@thread
                }
            }
            println("deleted zip file")
        }
    }

    private fun getChildren(file: FileDAO, user: UserDTO, namePrefix: String): Collection<Pair<File, String>> {
        val children = arrayListOf<Pair<File, String>>()
        FileDAO.find { FilesTable.parent eq file.id }.forEach { child ->
            if (!child.toFileDTO().canBeViewedByUserId(user.id)) {
                return@forEach
            }
            if (child.isFolder) {
                children.addAll(getChildren(child, user, namePrefix + child.name + "/"))
            }
            val systemFile = File("./${child.path}")
            if (systemFile.exists()) {
                children.add(Pair(systemFile, namePrefix + child.name))
            }
        }
        return children
    }

    fun deleteSingleFile(ctx: Context) {
        val file = ctx.attribute<FileDTO>("requestFileParameter")
        if (file == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
            throw NotFoundResponse("File not found")
        }

        val fileIDs = listOf(file.id)

        val deletedFileIDs = deleteFileList(fileIDs, ctx.currentUserDTO!!)

        val filesNotDeleted = fileIDs.filter { !deletedFileIDs.contains(it) }
        if (filesNotDeleted.isNotEmpty()) {
            ctx.json(mapOf("status" to "error", "filesNotDeleted" to filesNotDeleted, "filesDeleted" to deletedFileIDs))
        } else {
            ctx.json(mapOf("status" to "ok", "filesDeleted" to fileIDs))
        }

//        val systemFile = File("./${file.path}")
//        if (systemFile.exists()) {
//            systemFile.delete()
//        }
//        if (!systemFile.exists()) {
//            transaction {
//                FileDAO.findById(file.id)!!.delete()
//            }
//        }
//
//        //TODO: if file is a folder, delete all files in it
//
//        ctx.json(mapOf("status" to "ok"))
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

        val deletedFileIDs = deleteFileList(fileIDs, ctx.currentUserDTO!!)

        val filesNotDeleted = fileIDs.filter { !deletedFileIDs.contains(it) }
        if (filesNotDeleted.isNotEmpty()) {
            ctx.json(mapOf("status" to "error", "filesNotDeleted" to filesNotDeleted, "filesDeleted" to deletedFileIDs))
        } else {
            ctx.json(mapOf("status" to "ok", "filesDeleted" to fileIDs))
        }
    }

    private fun deleteFileList(fileIDs: List<UUID>, user: UserDTO): ArrayList<UUID> {
        val deletedFileIDs = arrayListOf<UUID>()
        transaction {
            FileDAO.find { FilesTable.id inList fileIDs }.forEach { file ->
                if (!file.isOwnedByUserId(user.id)) {
                    return@forEach
                }

                if (file.isFolder) {
                    logTimeSpent("recursively deleting folder ${file.name}") {
                        val folderFiles = FileDAO.find { FilesTable.parent eq file.id }.toList().map { it.id.value }
                        deleteFileList(folderFiles, user)
                        file.delete()
                        deletedFileIDs.add(file.id.value)
                    }
                } else {
                    logTimeSpent("deleting file ${file.name}") {
                        val systemFile = File("./${file.path}")
                        if (systemFile.exists()) {
                            systemFile.delete()
                        }
                        if (!systemFile.exists()) {
                            file.delete()
                            deletedFileIDs.add(file.id.value)
                        }
                    }
                }
            }
        }
        return deletedFileIDs
    }

    fun createDirectory(ctx: Context) {
        val folderName = ctx.formParamAsClass<String>("name").get()

        val parentId = if (ctx.queryParam("parent") != null) UUID.fromString(ctx.queryParam("parent")) else null

        transaction {
            val owner = UserDAO.find { UsersTable.id eq ctx.currentUserDTO!!.id }.first()
            val parent = if (parentId != null) FileDAO.findById(parentId) else null

            if (parent != null && !parent.toFileDTO().isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                throw BadRequestResponse("Parent folder does not exist or is not owned by you")
            }

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
}

private fun File.sha512(): String {
    val digest = MessageDigest.getInstance("SHA-512")
    val hash = digest.digest(this.readBytes())
    // convert byte array to Hex string
    val hexString = StringBuffer()
    for (i in hash.indices) {
        val hex = Integer.toHexString(0xff and hash[i].toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()
}

private fun String.isValidUUID(): Boolean {
    try {
        UUID.fromString(this)
    } catch (exception: IllegalArgumentException) {
        return false
    }
    return true
}
