package de.sam.base.tasks.types.files

import de.sam.base.config.Configuration
import de.sam.base.tasks.types.Task
import kotlinx.coroutines.delay
import me.desair.tus.server.TusFileUploadService
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.io.File

class FileCleanupTask : Task(name = "File Cleanup") {

    private val tusFileUploadSerivce: TusFileUploadService by inject()
    private val config: Configuration by inject()
    override suspend fun execute() {
        pushDescription("Starting file cleanup", true, 0)

        try {
            pushDescription("Starting tusFileUploadSerivce cleanup", true, 0)
            tusFileUploadSerivce.cleanup()
            pushDescription("Finished tusFileUploadSerivce cleanup successfully", true, 0)
        } catch (e: Exception) {
            Logger.error("Error while cleaning up tus uploads", e)
        }


        File(config.fileTempDirectory)
            .walk()
            .forEach {
                if (it.isFile) {
                    val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
                    if (it.lastModified() < oneHourAgo) {
                        pushDescription("Deleting file ${it.name}", true, 0)
                        it.delete()
                    }
                }
            }
        pushDescription("Finished file cleanup", true, 0)
        delay(2000)
    }
}