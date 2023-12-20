package de.sam.base

import de.sam.base.authentication.AuthenticationService
import de.sam.base.authentication.PasswordHasher
import de.sam.base.authentication.UserService
import de.sam.base.authentication.UserValidator
import de.sam.base.captcha.Captcha
import de.sam.base.config.Configuration
import de.sam.base.database.DatabaseManager
import de.sam.base.services.FileService
import de.sam.base.services.LoginLogService
import de.sam.base.services.ShareService
import de.sam.base.tasks.TaskController
import de.sam.base.tasks.queue.TaskQueue
import de.sam.base.utils.FileCache
import de.sam.base.utils.RateLimiter
import de.sam.base.utils.session.Session
import gg.jte.ContentType
import gg.jte.TemplateEngine
import me.desair.tus.server.TusFileUploadService
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.tinylog.kotlin.Logger
import java.io.File

fun main() {
    val configFile = File("./config.yml")
    if (!configFile.exists()) {
        Logger.warn("Config file not found, creating new one with defaults")
        Configuration.saveToFile(configFile)
    }
    val config = Configuration.fromFile(configFile)

    org.tinylog.configuration.Configuration.set("writer.level", config.logLevel)
    Logger.info("Log Level: " + config.logLevel)
    Logger.debug("Loaded config file")

    Logger.debug("Starting database connection")
    DatabaseManager(config).start()

    Logger.debug("Starting koin")
    startKoin {
        modules(module {
            single { config }
            single { RateLimiter() }
            single { LoginLogService() }
            single { FileService() }
            single { ShareService() }
            single { UserService() }
            single { UserValidator() }
            single { PasswordHasher() }
            single { AuthenticationService() }
            single { Session() }
            single { Captcha() }
            single { TaskQueue().withStartupTasks() }
            single { TaskController() }
            single {
                // https://github.com/casid/jte-javalin-tutorial/blob/d75d550cd6cd1dd33fcf461047851409e18a0525/src/main/java/app/App.java#L51
                // if dev mode
//                val codeResolver = DirectoryCodeResolver(Path.of("src", "main", "jte"))
//                return TemplateEngine.create(codeResolver, ContentType.Html)


                val templateEngine = TemplateEngine.createPrecompiled(ContentType.Html)
                templateEngine.setTrimControlStructures(true)
                templateEngine
            }
            single {
                TusFileUploadService()
                    .withStoragePath(config.tusTempDirectory)
                    .withUploadUri("/api/v1/files/upload")
                    .withUploadExpirationPeriod(1000 * 60 * 60) // 1 hour
            }
            single { FileCache() }
        })
    }

    Logger.debug("Starting webserver")
    WebServer().start()
    Logger.info("Started Successfully")
}