package de.sam.base.controllers

import com.google.common.hash.Hashing
import com.google.common.io.Files
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.*
import de.sam.base.services.FileService
import de.sam.base.utils.*
import de.sam.base.utils.file.zipFiles
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.*
import io.javalin.util.FileUtil
import jakarta.servlet.MultipartConfigElement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.tinylog.kotlin.Logger
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime

class FileController(private val fileService: FileService) {
    fun uploadFile(ctx: Context) {
        val maxFileSize = 1024L * 1024L * 1024L * 10L // 1024 MB || 10 GiB
        val fileSize = ctx.header(Header.CONTENT_LENGTH)?.toLong() ?: 0L

        if (fileSize > maxFileSize) {
            val error = "File size of ${humanReadableByteCountBin(fileSize)} exceeds the max allowed size of ${
                humanReadableByteCountBin(maxFileSize)
            }"
            throw BadRequestResponse(error)
        }

        val userId = ctx.currentUserDTO!!.id
        val parentFileId = ctx.queryParamAsClass<UUID>("parent").get()

        val files = try {
            ctx.req()
                .setAttribute(
                    "org.eclipse.jetty.multipartConfig",
                    MultipartConfigElement(config.fileTempDirectory, -1, -1, 1)
                )
            ctx.uploadedFiles()
        } catch (EofException: EOFException) {
            Logger.error("Early EOF, aborting request")
            throw BadRequestResponse("Early EOF")
        }
        val owner = ctx.currentUserDTO!!
        val parentFile = fileService.getFileById(parentFileId)

        if (parentFile != null && !parentFile.isOwnedByUserId(userId)) {
            throw BadRequestResponse("Parent folder does not exist or is not owned by you")
        }

        val idMap = mutableMapOf<String, UUID>()

        jdbi.useTransaction<Exception> { handle ->
            files.forEach {
                val uploadFolder = File(config.fileDirectory)
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdir()
                }
                val uploadTempFolder = File(config.fileTempDirectory)
                if (!uploadTempFolder.exists()) {
                    uploadTempFolder.mkdir()
                }

                val temporaryFileID = UUID.randomUUID()

                val temporaryFile = File("${config.fileTempDirectory}/${temporaryFileID}")

                logTimeSpent("downloading the file from the user") {
                    FileUtil.streamToFile(it.content(), temporaryFile.path)
                }

                // log error if temporary file could not be written
                if (!temporaryFile.exists()) {
                    Logger.error("Temporary file ${temporaryFile.path} could not be written")
                } else {
                    Logger.debug("Temporary file ${temporaryFile.path} written")
                }

                val hash = logTimeSpent("hashing file") {
                    temporaryFile.sha512()
                }

                val createdFile = fileService.createFile(
                    handle,
                    FileDTO(
                        id = UUID.randomUUID(),
                        name = it.filename(),
                        path = "upload/${UUID.randomUUID()}",
                        mimeType = it.contentType() ?: "application/octet-stream",
                        parent = parentFile!!.id,
                        owner = owner.id,
                        size = it.size(),
                        sizeHR = humanReadableByteCountBin(it.size()),
                        password = null,
                        private = false,
                        created = DateTime.now(),
                        isFolder = false,
                        isRoot = false,
                        hash = hash
                    )
                )

                // move file to upload folder with database id
                val targetFile = File("${config.fileDirectory}/${createdFile.id}")

                try {
                    java.nio.file.Files.move(
                        temporaryFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: IOException) {
                    Logger.error("Target file ${targetFile.path} could not be moved")

                    // delete the file from the database
                    fileService.deleteFilesAndShares(listOf(createdFile.id))

                    // delete the temporary file
                    temporaryFile.delete()

                    throw BadRequestResponse("Target file could not be moved")
                }
                Logger.debug("Target file ${targetFile.path} moved")

                idMap[createdFile.name] = createdFile.id
            }
            ctx.json(idMap)
        }
        if (ctx.header("x-batch-upload")?.toBoolean() == true) {
            val batchSize = ctx.header("x-batch-size")?.toInt() ?: 0
            val currentIndex = ctx.header("x-batch-index")?.toInt() ?: 0

            // do the calculation once when half of the files have been uploaded and once when the upload is finished
            if (batchSize / 2 == currentIndex + 1 || batchSize == currentIndex + 1) {
                Logger.debug("updating folder size during upload of batch ${currentIndex + 1}/${batchSize}")
                fileService.recalculateFolderSize(parentFileId, userId)
            }
        } else {
            Logger.debug("updating folder size during upload a singular file")
            fileService.recalculateFolderSize(parentFileId, userId)
        }
    }

    // from a share or from a file id
    fun getSingleFile(ctx: Context) {
        //TODO: this using a context extension

        val file = ctx.fileDTOFromId
            ?: fileService.getFileById(ctx.share!!.second.file)
            ?: throw NotFoundResponse("File not found") // if not throw an error
                .also { Logger.error(it.message) }

        val systemFile = File("${config.fileDirectory}/${file.id}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found")
        }

        val isDirectDownload = ctx.queryParam("download") != null

        // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
        val dispositionType = if (isDirectDownload) "attachment" else "inline"

        ctx.resultFile(systemFile, file.name, file.mimeType!!, dispositionType)
    }

    fun updateFile(ctx: Context) {
        val file = ctx.fileDTOFromId ?: throw NotFoundResponse("File not found")

        // the file is private and the user isn't logged in or the file isn't owned by the user
        if (!file.isOwnedByUserId(ctx.currentUserDTO?.id)) {
            throw NotFoundResponse("File not found")
        }

        // create a copy of the file DTO to hold the updated properties
        var updatedFile = file.copy()

        ctx.formParamMap().forEach { (key, value) ->
            when (key) {
                "name" -> updatedFile = updatedFile.copy(name = value.first())
//              "password" -> updatedFile = updatedFile.copy(password = value)
//              "public" -> updatedFile = updatedFile.copy(private = value.first() != "on" && value.first() != "true")
//              "parent" -> updatedFile = updatedFile.copy(parent = FileDAO.findById(UUID.fromString(value)))
            }
        }

        // use the file service to update the file
        fileService.updateFile(updatedFile) ?: throw NotFoundResponse("File not found")
    }

    private val usersCurrentlyZipping = mutableSetOf<UUID>()

    fun getFiles(ctx: Context) {
        val userId = ctx.currentUserDTO!!.id

        if (usersCurrentlyZipping.contains(userId)) {
            throw BadRequestResponse("You cannot download two zip files at once")
        }

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

        for (file in fileService.getFilesByIds(fileIDs)) {
            if (!file.isOwnedByUserId(userId)) {
                continue
            }

            if (file.isFolder) {
                // add all files and subfolders recursively
                fileList.addAll(getChildren(file, ctx.currentUserDTO!!, file.name + "/"))
            } else {
                val systemFile = File("${config.fileDirectory}/${file.id}")
                if (systemFile.exists()) {
                    fileList.add(Pair(systemFile, file.name))
                }
            }
        }
//        transaction {
//            FileDAO.find { FilesTable.id inList fileIDs }.forEach { file ->
//                if (!file.toDTO().isOwnedByUserId(ctx.currentUserDTO!!.id)) {
//                    return@forEach
//                }
//
//                if (file.isFolder) {
//                    // add all files and subfolders recursively
//                    fileList.addAll(getChildren(file, ctx.currentUserDTO!!, file.name + "/"))
//                } else {
//                    val systemFile = File("./${file.path}")
//                    if (systemFile.exists()) {
//                        fileList.add(Pair(systemFile, file.name))
//                    }
//                }
//            }
//        }

        val tempZipFile = File("${config.fileTempDirectory}/${UUID.randomUUID()}.zip")

        Logger.debug("Zipping ${fileList.size} files to ${tempZipFile.absolutePath}")

        // only let users create one zip at a time, reducing the possibility of a dos
        usersCurrentlyZipping.add(userId)

        val nanoTime = measureNanoTime {
            try {
                zipFiles(fileList, tempZipFile)
            } finally {
                usersCurrentlyZipping.remove(userId)
            }
        }

        val milliTime = nanoTime / 1000000.0
        Logger.debug("Zipping took $milliTime ms")
        Logger.debug("Zipping done")

        if (tempZipFile.exists()) {
            ctx.resultFile(
                tempZipFile,
                "download_${DateTime.now().toString("yyyy-MM-dd_HH-mm-ss")}.zip",
                "application/zip"
            )
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
            Logger.debug("deleted zip file")
        }
    }

    private fun getChildren(file: FileDTO, user: UserDTO, namePrefix: String): Collection<Pair<File, String>> {
        val children = arrayListOf<Pair<File, String>>()
        fileService.getFolderContentForUser(file.id, user.id).forEach { child ->
            if (child.isFolder) {
                children.addAll(getChildren(child, user, namePrefix + child.name + "/"))
            }
            val systemFile = File("${config.fileDirectory}/${child.id}")
            if (systemFile.exists()) {
                children.add(Pair(systemFile, namePrefix + child.name))
            }
        }
//        FileDAO.find { FilesTable.parent eq file.id }.forEach { child ->
//            if (!child.toDTO().isOwnedByUserId(user.id)) {
//                return@forEach
//            }
//            if (child.isFolder) {
//                children.addAll(getChildren(child, user, namePrefix + child.name + "/"))
//            }
//            val systemFile = File("./${child.path}")
//            if (systemFile.exists()) {
//                children.add(Pair(systemFile, namePrefix + child.name))
//            }
//        }
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

    fun deleteFileList(fileIDs: List<UUID>, user: UserDTO): List<UUID> {
        if (fileIDs.isEmpty()) {
            return emptyList()
        }

        val fileList = fileService.getFilesByIds(fileIDs).filter { it.isOwnedByUserId(user.id) }

        val filesToDelete = fileList.toMutableList()

        val folders = fileList.filter { it.isFolder }.map { it.id }

        // only do recursive step on folders
        if (folders.isNotEmpty()) {
            val recursiveFiles =
                fileService.getAllFilesFromFolderListRecursively(folders)

            filesToDelete.addAll(recursiveFiles)
        }

        val deletedFiles = fileService.deleteFilesAndShares(filesToDelete.map { it.id })

        deletedFiles.forEach { file ->
            val systemFile = File("${config.fileDirectory}/${file.id}")
            if (systemFile.exists()) {
                Logger.debug("Deleting file ${file.id}")
                systemFile.delete()
            }
        }

        fileList.groupBy { it.parent }
            .forEach { (parent) ->
                if (parent != null) {
                    val parentFile = fileService.getFileById(parent)
                    if (parentFile != null && parentFile.isFolder) {
                        Logger.debug("Recalculating folder size for ${parentFile.id}")
                        fileService.recalculateFolderSize(parentFile.id, user.id)
                    }
                }
            }


//        transaction {
//            allFiles.addAll(FileDAO.find { FilesTable.id inList fileIDs })
//        }
//
//        transaction {
//            allFiles.forEach { file ->
//                if (!file.isOwnedByUserId(user.id)) {
//                    return@forEach
//                }
//
//                fun deleteFile(file: FileDAO) {
//                    // delete all logs
//                    DownloadLogTable.deleteWhere { DownloadLogTable.file eq file.id }
//                    // delete all shares
//                    SharesTable.deleteWhere { SharesTable.file eq file.id }
//                    file.delete()
//                    deletedFileIDs.add(file.id.value)
//                }
//
//                if (file.isFolder) {
//                    logTimeSpent("recursively deleting folder ${file.name}") {
//                        val folderFiles = FileDAO.find { FilesTable.parent eq file.id }.toList().map { it.id.value }
//                        deleteFileList(folderFiles, user)
//                        deleteFile(file)
//                    }
//                } else {
//                    logTimeSpent("deleting file ${file.name}") {
//                        val systemFile = File("./${file.path}")
//                        if (systemFile.exists()) {
//                            systemFile.delete()
//                        }
//                        if (!systemFile.exists()) {
//                            deleteFile(file)
//                        }
//                    }
//                }
//                //TODO: only do this once (and last) if there are multiple files with the same parent
////                logTimeSpent("recalculating parent size") {
////                    if (file.parent != null) {
////                        if (file.parent != null) {
////                            file.parent!!.size = calculateFileSize(file.parent!!, user)
////                            file.parent!!.sizeHR = humanReadableByteCountBin(file.parent!!.size)
////                        }
////                    }
////                }
//            }
        //TODO: KEEP THIS
//            logTimeSpent("refreshing all deleted files parents size") {
//                val keys = allFiles.groupBy { it.parent }.keys
//
//                keys.forEach { parent ->
//                    if (parent != null) {
//                        Logger.debug("refreshing size of ${parent.name}")
//                        fileService.recalculateFolderSize(parent.id.value, user.id)
//                    }
//                }
//            }
//        }
        return deletedFiles.map { it.id }
    }

    fun moveFiles(ctx: Context) {
        val fileListString = ctx.formParamAsClass<String>("files")
            .check({ files ->
                files
                    .split(",")
                    .all { file ->
                        file.isValidUUID()
                    }
            }, "Invalid file UUID")
            .get()

        val targetFile = ctx.fileDAOFromId

        val fileIDs = fileListString.split(",").map { UUID.fromString(it) }

        val user = ctx.currentUserDTO!!

        val allFiles = arrayListOf<FileDAO>()
        var oldParents = listOf<FileDAO?>()
        transaction {
            allFiles.addAll(FileDAO.find { FilesTable.id inList fileIDs })
        }

        transaction {
            if (targetFile == null || !targetFile.isOwnedByUserId(user.id)) {
                return@transaction
            }

            oldParents = allFiles.map { it.parent }

            allFiles.forEach { file ->
                if (!file.isOwnedByUserId(user.id)) {
                    return@forEach
                }

                file.parent = targetFile
            }
        }

        logTimeSpent("refreshing all moved files parents size") {
            oldParents.forEach { parent ->
                if (parent != null) {
                    Logger.debug("refreshing size of ${parent.name}")
                    fileService.recalculateFolderSize(parent.id.value, user.id)
                }
            }

            fileService.recalculateFolderSize(targetFile!!.id.value, user.id)
        }
        ctx.json(mapOf("status" to "ok"))
    }


    fun createDirectory(ctx: Context) {
        val folderName = ctx.formParamAsClass<String>("name").get()

        val parentId = UUID.fromString(ctx.queryParam("parent"))

        transaction {
            val owner = UserDAO.find { UsersTable.id eq ctx.currentUserDTO!!.id }.first()
            val parent = FileDAO.findById(parentId)

            if (parent == null || !parent.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
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
                this.private = parent.private
                this.created = DateTime.now()
                this.isFolder = true
                this.isRoot = false
            }
            ctx.json(mapOf("id" to file.id.toString()))
        }
    }

    fun getFileMetadata(ctx: Context) {
        val file =
            transaction {
                ctx.fileDTOFromId // first check if the file is set by id
                    ?: FileDAO.findById(ctx.share!!.first.file.id)?.toDTO() // if not check if the file is set by share
                    ?: throw NotFoundResponse("File not found") // if not throw an error
                        .also { Logger.error(it.message) }
            }

        val shares = transaction {
            ShareDAO.find { SharesTable.file eq file.id }.map { it.toDTO() }
        }
        ctx.json(
            mapOf(
                "file" to file,
                "shares" to shares
            )
        )
    }

    fun getRootDirectory(ctx: Context) {
        ctx.json(
            fileService.getRootFolderForUser(ctx.currentUserDTO!!.id)
                ?: throw NotFoundResponse("Root directory not found")
        )
    }

    fun performFileSearch(ctx: Context) {
        val query = ctx.queryParam("q")

        if (query.isNullOrBlank()) {
            ctx.render("components/search/empty.kte")
            return
        }

        val files = fileService.searchFiles(ctx.currentUserDTO!!.id, query.trim())

        if (files.isEmpty()) {
            ctx.render("components/search/failed.kte")
            return
        }

        ctx.render("components/search/results.kte", Collections.singletonMap("files", files))
    }
}

val speedLimit = 1024.0 * 1024.0 * 10.0 // 10 MB/s

fun Context.resultFile(file: File, name: String, mimeType: String, dispositionType: String = "attachment") {
    // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
    this.header(Header.CONTENT_DISPOSITION, "$dispositionType; filename=$name")
    this.header(Header.CACHE_CONTROL, "max-age=31536000, immutable")

//    CustomSeekableWriter.write(this, ThrottledInputStream(FileInputStream(file), speedLimit), mimeType, file.length())
    CustomSeekableWriter.write(this, FileInputStream(file), mimeType, file.length())
}

private fun File.sha512(): String {

    // This is very memory intensive (loading the whole file into memory)
    /*
    val digest = MessageDigest.getInstance("SHA-512")
    val hash = digest.digest(this.readBytes())
    // convert byte array to Hex string
    val hexString = StringBuffer()
    for (i in hash.indices) {
        val hex = Integer.toHexString(0xff and hash[i].toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()*/

    // https://stackoverflow.com/a/31732451/11324248 with the note from https://stackoverflow.com/a/31732333/11324248
    return Files.asByteSource(this).hash(Hashing.sha512()).toString()
}

fun String.isValidUUID(): Boolean {
    try {
        UUID.fromString(this)
    } catch (exception: IllegalArgumentException) {
        return false
    }
    return true
}
