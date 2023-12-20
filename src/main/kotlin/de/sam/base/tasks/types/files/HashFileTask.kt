package de.sam.base.tasks.types.files

import com.google.common.hash.Hashing
import com.google.common.io.Files
import de.sam.base.config.Configuration
import de.sam.base.database.FileDTO
import de.sam.base.services.FileService
import de.sam.base.tasks.types.Task
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.system.measureTimeMillis

class HashFileTask(var file: FileDTO) : Task(name = "File hashing"), KoinComponent {
    val config: Configuration by inject()
    private val fileService: FileService by inject()

    override suspend fun execute() {
        pushDescription("Starting to hash file ${file.id}")
        val time = measureTimeMillis {
            val systemFile = File("${config.fileDirectory}/${file.id}")
            val hash = Files.asByteSource(systemFile).hash(Hashing.sha512()).toString()

            Logger.debug("File ${file.id} has hash $hash")

            file = file.copy(hash = hash)

            fileService.updateFile(file)
        }
        if (time < 100) {
            delay(200 - time)
        }
    }
}