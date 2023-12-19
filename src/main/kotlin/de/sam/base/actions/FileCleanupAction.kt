package de.sam.base.actions

import de.sam.base.config.Configuration
import me.desair.tus.server.TusFileUploadService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.io.File

class FileCleanupAction : KoinComponent {
    private val tusFileUploadSerivce: TusFileUploadService by inject()
    private val config: Configuration by inject()
    fun cleanup() {
        Logger.info("Starting file cleanup")

        try {
            Logger.info("Starting tusFileUploadSerivce cleanup")
            tusFileUploadSerivce.cleanup()
            Logger.info("Finished tusFileUploadSerivce cleanup successfully")
        } catch (e: Exception) {
            Logger.error("Error while cleaning up tus uploads", e)
        }

        File(config.fileTempDirectory)
            .walk()
            .forEach {
                if (it.isFile) {
                    val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
                    if (it.lastModified() < oneHourAgo) {
                        Logger.debug("Deleting file ${it.name}")
                        it.delete()
                    }
                }
            }
        Logger.info("Finished file cleanup")
    }
}