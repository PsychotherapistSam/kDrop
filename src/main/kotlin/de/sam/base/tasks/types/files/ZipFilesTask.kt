package de.sam.base.tasks.types.files

import de.sam.base.config.Configuration
import de.sam.base.database.FileDTO
import de.sam.base.database.UserDTO
import de.sam.base.services.FileService
import de.sam.base.tasks.types.Task
import de.sam.base.utils.file.zipFiles
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*
import kotlin.system.measureNanoTime

class ZipFilesTask(private val user: UserDTO, private val fileIDs: List<UUID>) : Task(name = "Zip files"),
    KoinComponent {

    private val config: Configuration by inject()
    private val fileService: FileService by inject()

    lateinit var tempZipFile: File

    override suspend fun execute() {
        val userId = user.id

//        pushDescription("Starting to zip files", true, 0)

        // file, name / path inside zip
        val fileList = arrayListOf<Pair<File, String>>()

        for (file in fileService.getFilesByIds(fileIDs)) {
            if (!file.isOwnedByUserId(userId)) {
                continue
            }

            if (file.isFolder) {
                // add all files and subfolders recursively
                fileList.addAll(getChildren(file, user, file.name + "/"))
            } else {
                val systemFile = File("${config.fileDirectory}/${file.id}")
                if (systemFile.exists()) {
                    fileList.add(Pair(systemFile, file.name))
                }
            }
        }

        tempZipFile = File("${config.fileTempDirectory}/${UUID.randomUUID()}.zip")

        pushDescription("Zipping ${fileList.size} files to ${tempZipFile.absolutePath}", true, 0)

        // only let users create one zip at a time, reducing the possibility of a dos
//        usersCurrentlyZipping.add(userId)

        val nanoTime = measureNanoTime {
            try {
                zipFiles(fileList, tempZipFile)
            } finally {
//                usersCurrentlyZipping.remove(userId)
            }
        }

        val milliTime = nanoTime / 1000000.0
        pushDescription("Zipping took $milliTime ms", true, 0)
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
        return children
    }
}