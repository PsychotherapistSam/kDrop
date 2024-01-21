package de.sam.base.controllers

import com.google.common.hash.Hashing
import com.google.common.io.Files
import de.sam.base.authentication.PasswordHasher
import de.sam.base.config.Configuration
import de.sam.base.database.FileDTO
import de.sam.base.database.jdbi
import de.sam.base.services.FileService
import de.sam.base.services.ShareService
import de.sam.base.tasks.queue.TaskQueue
import de.sam.base.tasks.types.files.HashFileTask
import de.sam.base.tasks.types.files.ZipFilesTask
import de.sam.base.utils.*
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.*
import io.javalin.util.FileUtil
import me.desair.tus.server.TusFileUploadService
import me.desair.tus.server.exception.UploadNotFoundException
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread

class FileController : KoinComponent {

    private val config: Configuration by inject()
    private val fileService: FileService by inject()
    private val shareService: ShareService by inject()
    private val passwordHasher: PasswordHasher by inject()
    private val tusFileUploadSerivce: TusFileUploadService by inject()
    private val taskQueue: TaskQueue by inject()

    fun handleTUSUpload(ctx: Context) {
        val servletRequest = ctx.req()
        val servletResponse = ctx.res()

        tusFileUploadSerivce.process(servletRequest, servletResponse)

        val uploadURI = servletRequest.requestURI
        val uploadInfo = tusFileUploadSerivce.getUploadInfo(uploadURI)

        if (uploadInfo != null && !uploadInfo.isUploadInProgress) {
            val uploadId = UUID.fromString(uploadInfo.id.toString())

            val userId = ctx.currentUserDTO!!.id
            val parentFileId = UUID.fromString(ctx.header("X-File-Parent-Id"))

            val parentFile = fileService.getFileById(parentFileId)
            if (parentFile != null && !parentFile.isOwnedByUserId(userId)) {
                tusFileUploadSerivce.deleteUpload(uploadURI)
                throw BadRequestResponse("Parent folder does not exist or is not owned by you")
            }

            jdbi.useTransaction<Exception> { handle ->
                val uploadFolder = File(config.fileDirectory)
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdir()
                }
                val uploadTempFolder = File(config.fileTempDirectory)
                if (!uploadTempFolder.exists()) {
                    uploadTempFolder.mkdir()
                }

                val targetFile = File("${config.fileDirectory}/${uploadId}")

                logTimeSpent("downloading the file from the user") {
                    try {
                        FileUtil.streamToFile(tusFileUploadSerivce.getUploadedBytes(uploadURI), targetFile.path)
                    } catch (e: UploadNotFoundException) {
                        throw BadRequestResponse("Upload not found")
                    }
                }

                if (!targetFile.exists()) {
                    Logger.error("Target file ${targetFile.path} could not be written")
                } else {
                    Logger.debug("Target file ${targetFile.path} written")
                }

                val createdFile = fileService.createFile(
                    handle, FileDTO(
                        id = uploadId,
                        name = uploadInfo.fileName,
                        path = "upload/${UUID.randomUUID()}",
                        mimeType = uploadInfo.fileMimeType ?: "application/octet-stream",
                        parent = parentFile!!.id,
                        owner = ctx.currentUserDTO!!.id,
                        size = targetFile.length(),
                        sizeHR = humanReadableByteCountBin(targetFile.length()),
                        password = null,
                        created = DateTime.now(),
                        isFolder = false,
                        isRoot = false,
                        hash = null
                    )
                )

                if (config.hashing.enabled && config.hashing.onUpload) {
                    Logger.debug("Hashing file ${targetFile.path} since it was uploaded")
                    taskQueue.enqueueTask(HashFileTask(createdFile))
                } else {
                    Logger.debug("Skipping hashing since it is disabled")
                }

                Logger.debug("Target file ${targetFile.path} moved")
                tusFileUploadSerivce.deleteUpload(uploadURI)
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
        ctx.status(servletResponse.status)
    }

    // from a share or from a file id
    fun getSingleFile(ctx: Context) {
        val file = ctx.fileDTOFromId ?: fileService.getFileById(ctx.share!!.file)
        ?: throw NotFoundResponse("File not found") // if not throw an error
            .also { Logger.error(it.message) }


        val isShareRequest = ctx.share != null
        if (isShareRequest && ctx.share!!.password != null) {
            // verify password
            val providedPassword = ctx.queryParam("password") ?: throw BadRequestResponse("No password provided")
            val passwordCorrect =
                passwordHasher.verifyPassword(
                    providedPassword!!, ctx.share!!.password!!, file.id.toString()
                )

            if (!passwordCorrect) {
                throw BadRequestResponse("Wrong password")
            }
        }

        val systemFile = File("${config.fileDirectory}/${file.id}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found")
        }

        val isDirectDownload = ctx.queryParam("download") != null

        // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
        val dispositionType = if (isDirectDownload) "attachment" else "inline"

        ctx.resultFile(systemFile, file.name, file.mimeType!!, dispositionType)

        if (isShareRequest) {
            ctx.share!!.downloadCount++
            shareService.updateShareDownloadCount(ctx.share!!)

            if (ctx.share!!.maxDownloads != null && ctx.share!!.maxDownloads!! > 0 && ctx.share!!.downloadCount >= ctx.share!!.maxDownloads!!) {
                shareService.deleteShare(ctx.share!!.id)
            }
        }
    }

    fun updateFile(ctx: Context) {
        val file = ctx.fileDTOFromId ?: throw NotFoundResponse("File not found")

        if (!file.isOwnedByUserId(ctx.currentUserDTO?.id)) {
            throw NotFoundResponse("File not found")
        }

        // create a copy of the file DTO to hold the updated properties
        var updatedFile = file.copy()

        ctx.formParamMap().forEach { (key, value) ->
            when (key) {
                "name" -> updatedFile = updatedFile.copy(name = value.first())
//              "password" -> updatedFile = updatedFile.copy(password = value)
//              "parent" -> updatedFile = updatedFile.copy(parent = FileDAO.findById(UUID.fromString(value)))
            }
        }

        // use the file service to update the file
        fileService.updateFile(updatedFile) ?: throw NotFoundResponse("File not found")
    }

    fun getFiles(ctx: Context) {
        val fileListString = ctx.formParamAsClass<String>("files").check({ files ->
            files.split(",").all { file ->
                file.isValidUUID()
            }
        }, "Invalid UUID").get()
        val fileIDs = fileListString.split(",").map { UUID.fromString(it) }

        val hashFileTask = ZipFilesTask(ctx.currentUserDTO!!, fileIDs)

        taskQueue.enqueueTask(hashFileTask)

        ctx.future {
            hashFileTask.hasFinished.thenAccept {
                if (!hashFileTask.tempZipFile.exists()) {
                    throw NotFoundResponse("File not found")
                }
                ctx.resultFile(
                    hashFileTask.tempZipFile,
                    "download_${DateTime.now().toString("yyyy-MM-dd_HH-mm-ss")}.zip",
                    "application/zip"
                )
                thread {
                    var counter = 0
                    while (hashFileTask.tempZipFile.exists()) {
                        hashFileTask.tempZipFile.delete()
                        counter++
                        sleep(1000)
                        // if file can not be deleted at runtime, it will be deleted on the next stop
                        if (counter > 60) {
                            hashFileTask.tempZipFile.deleteOnExit()
                            return@thread
                        }
                    }
                    Logger.debug("deleted zip file")
                }
            }?.exceptionally {
                throw InternalServerErrorResponse("Zipping failed failed")
            }!!
        }
    }


    fun deleteSingleFile(ctx: Context) {
        val file = ctx.attribute<FileDTO>("requestFileParameter")
        if (file == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
            throw NotFoundResponse("File not found")
        }

        val fileIDs = listOf(file.id)

        val deletedFileIDs = deleteFileList(fileIDs, ctx.currentUserDTO!!.id)

        val filesNotDeleted = fileIDs.filter { !deletedFileIDs.contains(it) }
        if (filesNotDeleted.isNotEmpty()) {
            ctx.json(mapOf("status" to "error", "filesNotDeleted" to filesNotDeleted, "filesDeleted" to deletedFileIDs))
        } else {
            ctx.json(mapOf("status" to "ok", "filesDeleted" to fileIDs))
        }
    }

    fun deleteFiles(ctx: Context) {
        val fileListString = ctx.formParamAsClass<String>("files").check({ files ->
            files.split(",").all { file ->
                file.isValidUUID()
            }
        }, "Invalid UUID").get()

        val fileIDs = fileListString.split(",").map { UUID.fromString(it) }

        val deletedFileIDs = deleteFileList(fileIDs, ctx.currentUserDTO!!.id)

        val filesNotDeleted = fileIDs.filter { !deletedFileIDs.contains(it) }
        if (filesNotDeleted.isNotEmpty()) {
            ctx.json(mapOf("status" to "error", "filesNotDeleted" to filesNotDeleted, "filesDeleted" to deletedFileIDs))
        } else {
            ctx.json(mapOf("status" to "ok", "filesDeleted" to fileIDs))
        }
    }

    fun deleteFileList(fileIDs: List<UUID>, userId: UUID): List<UUID> {
        if (fileIDs.isEmpty()) {
            return emptyList()
        }

        val fileList = fileService.getFilesByIds(fileIDs).filter { it.isOwnedByUserId(userId) }

        val filesToDelete = fileList.toMutableList()

        val folders = fileList.filter { it.isFolder }.map { it.id }

        // only do recursive step on folders
        if (folders.isNotEmpty()) {
            val recursiveFiles = fileService.getAllFilesFromFolderListRecursively(folders)

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

        fileList.groupBy { it.parent }.forEach { (parent) ->
            if (parent != null) {
                val parentFile = fileService.getFileById(parent)
                if (parentFile != null && parentFile.isFolder) {
                    Logger.debug("Recalculating folder size for ${parentFile.id}")
                    fileService.recalculateFolderSize(parentFile.id, userId)
                }
            }
        }
        return deletedFiles.map { it.id }
    }

    fun moveFiles(ctx: Context) {
        val fileListString = ctx.formParamAsClass<String>("files").check({ files ->
            files.split(",").all { file ->
                file.isValidUUID()
            }
        }, "Invalid file UUID").get()

        val targetFile = ctx.fileDTOFromId

        val fileIDs = fileListString.split(",").map { UUID.fromString(it) }

        val user = ctx.currentUserDTO!!

        val allFiles = fileService.getFilesByIds(fileIDs)
            .filter { it.isOwnedByUserId(user.id) }

        if (targetFile == null || !targetFile.isOwnedByUserId(user.id)) {
            return
        }

        val oldParents: List<UUID?> = allFiles.map { it.parent }.distinct()

        val updatedFiles = allFiles.map {
            it.copy(parent = targetFile.id)
        }

        logTimeSpent("updating ${updatedFiles.size} moved files parents") {
            fileService.updateFilesBatch(updatedFiles)
        }

        logTimeSpent("refreshing all moved files parents size") {
            oldParents.forEach { parent ->
                if (parent != null) {
                    Logger.debug("refreshing size of $parent")
                    fileService.recalculateFolderSize(parent, user.id)
                }
            }
            fileService.recalculateFolderSize(targetFile.id, user.id)
        }
        ctx.json(mapOf("status" to "ok"))
    }

    fun hashFile(ctx: Context) {
        val file = ctx.fileDTOFromId ?: throw NotFoundResponse("File not found")

        if (!file.isOwnedByUserId(ctx.currentUserDTO?.id)) {
            throw NotFoundResponse("File not found")
        }

        val systemFile = File("${config.fileDirectory}/${file.id}")
        if (!systemFile.exists()) {
            throw NotFoundResponse("File not found")
        }

        if (file.hash != null) {
            ctx.html(file.hash!!)
            return
        }

        val hashFileTask = HashFileTask(file)
        taskQueue.enqueueTask(hashFileTask)

        ctx.future {
            hashFileTask.hasFinished.thenAccept {
                ctx.html(hashFileTask.file.hash!!)
            }?.exceptionally {
                ctx.status(500).result("Hashing failed")
                null
            }!!
        }
    }


    fun createDirectory(ctx: Context) {
        val folderName = ctx.formParamAsClass<String>("name").get()

        val parentId = UUID.fromString(ctx.queryParam("parent"))

        val parent = fileService.getFileById(parentId)

        if (parent == null || !parent.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
            throw BadRequestResponse("Parent folder does not exist or is not owned by you")
        }

        val newFileId = UUID.randomUUID()
        jdbi.useTransaction<Exception> { handle ->
            val file =
                fileService.createFile(
                    handle,
                    FileDTO(
                        id = newFileId,
                        name = folderName,
                        path = "upload/${newFileId}",
                        mimeType = "",
                        parent = parent.id,
                        owner = ctx.currentUserDTO!!.id,
                        size = 0,
                        sizeHR = "0 B",
                        password = null,
                        created = DateTime.now(),
                        isFolder = true,
                        isRoot = false
                    )
                )
            ctx.json(mapOf("id" to file.id.toString()))
        }
    }

    fun getFileMetadata(ctx: Context) {
        val file: FileDTO =
            if (ctx.fileDTOFromId != null) {
                ctx.fileDTOFromId!!
            } else {
                val fileId = ctx.queryParamAsClass<UUID>("file").get()
                fileService.getFileById(fileId) ?: throw NotFoundResponse("File not found")
            }

        val shares = shareService.getSharesForFile(file.id)

        ctx.json(
            mapOf(
                "file" to file, "shares" to shares
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

        if (query.isNullOrBlank() || query.length < 3) {
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
