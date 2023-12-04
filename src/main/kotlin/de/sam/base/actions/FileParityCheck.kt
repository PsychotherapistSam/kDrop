package de.sam.base.actions

import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.isValidUUID
import de.sam.base.services.FileService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*

class FileParityCheck : KoinComponent {
    private val fileService: FileService by inject()
    fun checkIfLocalFilesExistInDatabase() {
        val localFilesList = File(config.fileDirectory)
            .walk()
            .toList()
            .map { it.name }
            .filter { it.isValidUUID() }
            .map { UUID.fromString(it) }

        val existingFiles = fileService.getFilesByIds(localFilesList)
            .map { it.id }

        val filesNotInDB = localFilesList.filter { !existingFiles.contains(it) }

        filesNotInDB.forEach {
            println("File $it does not exist in the database, deleting it locally...")
            File("${config.fileDirectory}/$it").delete()
        }
    }
}