package de.sam.base.controllers

import com.google.common.hash.Hashing
import com.google.common.io.Files
import de.sam.base.database.*
import de.sam.base.utils.*
import de.sam.base.utils.file.zipFiles
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.*
import io.javalin.util.FileUtil
import jakarta.servlet.MultipartConfigElement
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import org.joda.time.DateTime
import org.tinylog.kotlin.Logger
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class FileController {
    fun uploadFile(ctx: Context) {
        val maxFileSize = 1024L * 1024L * 1024L * 10L // 1024 MB || 10 GiB
        if (ctx.header(Header.CONTENT_LENGTH) != null && ctx.header(Header.CONTENT_LENGTH)!!.toLong() > maxFileSize) {
            throw BadRequestResponse("File too big, max size is ${humanReadableByteCountBin(maxFileSize)}")
        }

        val userId = ctx.currentUserDTO!!.id
        val parentFileId = ctx.queryParamAsClass<UUID>("parent").get()

        val files = try {
            ctx.req()
                .setAttribute(
                    "org.eclipse.jetty.multipartConfig",
                    MultipartConfigElement(File("./upload/temp").absolutePath, -1, -1, 1)
                )
            ctx.uploadedFiles()
        } catch (EofException: EOFException) {
            Logger.error("Early EOF, aborting request")
            throw BadRequestResponse("Early EOF")
        }
        transaction {
            val owner = ctx.currentUserDTO!!.fetchDAO()!!
            val parentFile = FileDAO.findById(parentFileId)

            if (parentFile != null && !parentFile.toDTO().isOwnedByUserId(userId)) {
                throw BadRequestResponse("Parent folder does not exist or is not owned by you")
            }

            val idMap = mutableMapOf<String, UUID>()

            files.forEach {
                val uploadFolder = File("./upload/")
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdir()
                }

                val file = FileDAO.new {
                    this.name = it.filename()
                    this.path = "upload/${this.id}"
                    this.mimeType = it.contentType() ?: "application/octet-stream"
                    this.parent = parentFile
                    this.owner = owner
                    this.size = it.size()
                    this.sizeHR = humanReadableByteCountBin(it.size())
                    this.password = null
                    this.private = parentFile?.private ?: false
                    this.created = DateTime.now()
                    this.isFolder = false
                    this.isRoot = false
                }

                val targetFile = File("./upload/${file.id}")

                logTimeSpent("downloading the file from the user") {
                    FileUtil.streamToFile(it.content(), targetFile.path)
                }
                logTimeSpent("hashing file") {
                    file.hash = targetFile.sha512()
                }

                idMap[file.name] = file.id.value
            }
            ctx.json(idMap)
        }
        if (ctx.header("x-batch-upload")?.toBoolean() == true) {
            val batchSize = ctx.header("x-batch-size")?.toInt() ?: 0
            val currentIndex = ctx.header("x-batch-index")?.toInt() ?: 0

            // do the calculation once when half of the files have been uploaded and once when the upload is finished
            if (batchSize / 2 == currentIndex + 1 || batchSize == currentIndex + 1) {
                Logger.debug("updating folder size during upload of batch ${currentIndex + 1}/${batchSize}")
                recalculateFolderSize(parentFileId, userId)
            }
        } else {
            Logger.debug("updating folder size during upload a singular file")
            recalculateFolderSize(parentFileId, userId)
        }
        try {
        } catch (ex: ExposedSQLException) {
            // ExposedSQLException -> PSQLException
            // this happens when multiple threads try to recalculate the size of the same folder at the same time
            // error message is  ERROR: could not serialize access due to concurrent update
            Logger.error(ex.message)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun recalculateFolderSize(folderId: UUID, userId: UUID) {
        // this is a seperate transaction beacuse the files would not be in the database
        transaction {
            measureTime {
                val owner = UserDAO.findById(userId)!!
                val parent = FileDAO.findById(folderId)

                if (parent != null) {
                    //TODO: also maybe update parents parent recursively // this ends up in nearly going to the root folder.

                    // update lowest most folder since it's not included in the above loop
                    //                Logger.info("updating folder ${parent.name}")
                    //                parent.size = getFileSize(parent, owner.toUser()) // + files.sumByLong { it.size }
                    //                parent.sizeHR = humanReadableByteCountBin(parent.size)
                    //                parent.created = DateTime.now()

                    // find upper most parent and update it's size including the newly uploaded files (assuming they were all successfully uploaded)
                    // ideally this should be in a seperate transaction.
                    var parentsParent = parent
                    while (parentsParent != null) {
                        Logger.debug("updating folder ${parentsParent.name} which was ${parentsParent.sizeHR}")
                        parentsParent.size = calculateFileSize(parentsParent, owner.toDTO())
                        // + files.sumByLong { it.size }
                        parentsParent.sizeHR = humanReadableByteCountBin(parentsParent.size)
                        Logger.debug("folder is now ${parentsParent.sizeHR}")
                        parentsParent.created = DateTime.now()

                        parentsParent = parentsParent.parent
                    }
                }
            }
        }.let { Logger.debug("refreshed filesize tree in ${it.toLong(DurationUnit.MILLISECONDS)}ms") }
    }

    private fun calculateFileSize(file: FileDAO, user: UserDTO): Long {
        var size = 0L
        size +=
            if (file.isFolder) {
                getAllChildrenRecursively(file, user).sumByLong { it.size }
            } else {
                file.size
            }
        return size
    }

    private fun getAllChildrenRecursively(file: FileDAO, user: UserDTO): ArrayList<FileDAO> {
        val children = arrayListOf<FileDAO>()
        logTimeSpent("getting children of ${file.name}") {
            FileDAO.find { FilesTable.parent eq file.id }.forEach { child ->
                if (!child.toDTO().isOwnedByUserId(user.id)) {
                    return@forEach
                }
                children.add(child)
            }
        }
        return children
    }

    fun getSingleFile(ctx: Context) {
        //TODO: this using a context extension

        val file =
            transaction {
                ctx.fileDTOFromId // first check if the file is set by id
                    ?: FileDAO.findById(ctx.share!!.first.file.id)?.toDTO() // if not check if the file is set by share
                    ?: throw NotFoundResponse("File not found") // if not throw an error
                        .also { Logger.error(it.message) }
            }

        val systemFile = File("./${file.path}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found")
        }
        val isDirectDownload = ctx.queryParam("download") != null

        // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
        val dispositionType = if (isDirectDownload) "attachment" else "inline"

//        ctx.header(Header.DATE, file.created.toString())
//        if (file.hash != null) {
//            ctx.header("SHA512", file.hash!!)
//        }

        //            if (isDirectDownload) {
//                transaction {
//                    logTimeSpent("adding file log entry") {
//                        DownloadLogDAO.new {
//                            this.file = ctx.fileDAOFromId
//                            this.user = ctx.currentUserDTO?.fetchDAO()
//                            this.ip = ctx.ip()
//                            this.readDuration = System.nanoTime() - ctx.requestStartTime
//                            this.downloadDate = DateTime.now() - (this.readDuration / 1000000L)
//                            this.readBytes = file.size
//                            this.userAgent = ctx.header(Header.USER_AGENT) ?: "unknown"
//                        }
//                    }
//                }
//            }

        ctx.resultFile(systemFile, file.name, file.mimeType, dispositionType)
    }

    fun updateFile(ctx: Context) {
        val file = ctx.fileDTOFromId ?: throw NotFoundResponse("File not found")

        // the file is private and the user isn't logged in or the file isn't owned by the user
        if (!file.isOwnedByUserId(ctx.currentUserDTO?.id)) {
            throw NotFoundResponse("File not found")
        }
        transaction {
            val actualFile = FileDAO.findById(file.id) ?: throw NotFoundResponse("File not found")
            ctx.formParamMap().forEach { (key, value) ->
                when (key) {
                    "name" -> actualFile.name = value.first()
//                    "password" -> actualFile.password = value
                    "public" -> actualFile.private = value.first() != "on" && value.first() != "true"
//                    "parent" -> actualFile.parent = FileDAO.findById(UUID.fromString(value))
                }
            }
        }
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
        transaction {
            FileDAO.find { FilesTable.id inList fileIDs }.forEach { file ->
                if (!file.toDTO().isOwnedByUserId(ctx.currentUserDTO!!.id)) {
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

    private fun getChildren(file: FileDAO, user: UserDTO, namePrefix: String): Collection<Pair<File, String>> {
        val children = arrayListOf<Pair<File, String>>()
        FileDAO.find { FilesTable.parent eq file.id }.forEach { child ->
            if (!child.toDTO().isOwnedByUserId(user.id)) {
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

    fun deleteFileList(fileIDs: List<UUID>, user: UserDTO): ArrayList<UUID> {
        val deletedFileIDs = arrayListOf<UUID>()
        val allFiles = arrayListOf<FileDAO>()
        transaction {
            allFiles.addAll(FileDAO.find { FilesTable.id inList fileIDs })
        }

        transaction {
            allFiles.forEach { file ->
                if (!file.isOwnedByUserId(user.id)) {
                    return@forEach
                }

                fun deleteFile(file: FileDAO) {
                    // delete all logs
                    DownloadLogTable.deleteWhere { DownloadLogTable.file eq file.id }
                    // delete all shares
                    SharesTable.deleteWhere { SharesTable.file eq file.id }
                    file.delete()
                    deletedFileIDs.add(file.id.value)
                }

                if (file.isFolder) {
                    logTimeSpent("recursively deleting folder ${file.name}") {
                        val folderFiles = FileDAO.find { FilesTable.parent eq file.id }.toList().map { it.id.value }
                        deleteFileList(folderFiles, user)
                        deleteFile(file)
                    }
                } else {
                    logTimeSpent("deleting file ${file.name}") {
                        val systemFile = File("./${file.path}")
                        if (systemFile.exists()) {
                            systemFile.delete()
                        }
                        if (!systemFile.exists()) {
                            deleteFile(file)
                        }
                    }
                }
                //TODO: only do this once (and last) if there are multiple files with the same parent
//                logTimeSpent("recalculating parent size") {
//                    if (file.parent != null) {
//                        if (file.parent != null) {
//                            file.parent!!.size = calculateFileSize(file.parent!!, user)
//                            file.parent!!.sizeHR = humanReadableByteCountBin(file.parent!!.size)
//                        }
//                    }
//                }
            }
            logTimeSpent("refreshing all deleted files parents size") {
                val keys = allFiles.groupBy { it.parent }.keys

                keys.forEach { parent ->
                    if (parent != null) {
                        Logger.debug("refreshing size of ${parent.name}")
                        recalculateFolderSize(parent.id.value, user.id)
                    }
                }
            }
        }
        return deletedFileIDs
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
        transaction {
            allFiles.addAll(FileDAO.find { FilesTable.id inList fileIDs })
        }

        transaction {
            if (targetFile == null || !targetFile.isOwnedByUserId(user.id)) {
                return@transaction
            }

            val oldParents = allFiles.map { it.parent }.toSet()

            allFiles.forEach { file ->
                if (!file.isOwnedByUserId(user.id)) {
                    return@forEach
                }

                file.parent = targetFile
            }

            logTimeSpent("refreshing all moved files parents size") {
                oldParents.forEach { parent ->
                    if (parent != null) {
                        Logger.debug("refreshing size of ${parent.name}")
                        recalculateFolderSize(parent.id.value, user.id)
                    }
                }

                recalculateFolderSize(targetFile.id.value, user.id)
            }
            ctx.json(mapOf("status" to "ok"))
        }
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
