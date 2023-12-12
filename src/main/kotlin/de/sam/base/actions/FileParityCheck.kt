package de.sam.base.actions

import de.sam.base.config.Configuration
import de.sam.base.controllers.isValidUUID
import de.sam.base.services.FileService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.io.File
import java.util.*

class FileParityCheck : KoinComponent {
    private val fileService: FileService by inject()
    private val config: Configuration by inject()

    fun checkIfLocalFilesExistInDatabase() {
        val localFilesList = File(config.fileDirectory)
            .walk()
            .toList()
            .map { it.name }
            .filter { it.isValidUUID() }
            .map { UUID.fromString(it) }

        if (localFilesList.isEmpty()) {
            Logger.info("No files found in the file directory, skipping parity check")
            return
        }

        val existingFiles = fileService.getFilesByIds(localFilesList)
            .map { it.id }

        val filesNotInDB = localFilesList.filter { !existingFiles.contains(it) }

        filesNotInDB.forEach {
            Logger.info("File $it is not in the database, deleting it")
            File("${config.fileDirectory}/$it").delete()
        }
    }
}