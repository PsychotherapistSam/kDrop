package de.sam.base.actions

import de.sam.base.config.Configuration.Companion.config
import org.tinylog.kotlin.Logger
import java.io.File

class FileCleanupAction {
    fun cleanup() {
        Logger.info("Starting file cleanup")
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