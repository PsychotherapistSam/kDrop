package de.sam.base

import de.sam.base.actions.FileCleanupAction
import de.sam.base.actions.FileParityCheck
import de.sam.base.authentication.AuthenticationService
import de.sam.base.authentication.PasswordHasher
import de.sam.base.authentication.UserService
import de.sam.base.authentication.UserValidator
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.DatabaseManager
import de.sam.base.services.FileService
import de.sam.base.services.LoginLogService
import de.sam.base.services.ShareService
import de.sam.base.utils.logging.logTimeSpent
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.tinylog.kotlin.Logger
import java.io.File

fun main() {
    val configFile = File("./config.yml")
    if (!configFile.exists()) {
        Logger.warn("Config file not found, creating new one with defaults")
        config.saveToFile(configFile)
    }
    config.loadFromFile(configFile)
    org.tinylog.configuration.Configuration.set("writer.level", config.logLevel)
    Logger.info("Log Level: " + config.logLevel)
    Logger.debug("Loaded config file")

    Logger.debug("Starting database connection")
    DatabaseManager().start()

    Logger.debug("Starting koin")
    startKoin {
        modules(module {
            single<LoginLogService> { LoginLogService() }
            single<FileService> { FileService() }
            single<ShareService> { ShareService() }
            single<UserService> { UserService() }
            single<UserValidator> { UserValidator() }
            single<PasswordHasher> { PasswordHasher() }
            single<AuthenticationService> { AuthenticationService() }
        })
    }

    Logger.debug("Starting webserver")
    WebServer().start()
    Logger.info("Started Successfully")

    logTimeSpent("Checking for files that do not exist in the database") {
        FileParityCheck().checkIfLocalFilesExistInDatabase()
    }
    logTimeSpent("Cleaning up temporary files") {
        FileCleanupAction().cleanup()
    }
}