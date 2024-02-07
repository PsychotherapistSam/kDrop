package de.sam.base.tasks.types.files

import de.sam.base.config.Configuration
import de.sam.base.file.repository.FileRepository
import de.sam.base.tasks.types.Task
import de.sam.base.utils.string.isUUID
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*


class FileParityCheckTask : Task(name = "Parity check", concurrency = 1), KoinComponent {
    private val fileRepository: FileRepository by inject()
    private val config: Configuration by inject()
    override suspend fun execute() {
        pushDescription("Checking if local files exist in database")

        val localFilesList = File(config.fileDirectory)
            .walk()
            .toList()
            .map { it.name }
            .filter { it.isUUID }
            .map { UUID.fromString(it) }

        if (localFilesList.isEmpty()) {
            pushDescription("No files found in the file directory, skipping parity check", true, 0)
            return
        }

        val existingFiles = fileRepository.getFilesByIds(localFilesList)
            .map { it.id }

        val filesNotInDB = localFilesList.filter { !existingFiles.contains(it) }

        filesNotInDB.forEach {
            pushDescription("File $it is not in the database, deleting it", true, 0)
            File("${config.fileDirectory}/$it").delete()
        }

        delay(2000)
    }
}