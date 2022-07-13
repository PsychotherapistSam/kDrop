package de.sam.base

import de.sam.base.config.Configuration
import de.sam.base.database.DatabaseManager
import org.tinylog.kotlin.Logger
import java.io.File

fun main(args: Array<String>) {
    Logger.debug("Loading config file")
    val configFile = File("./config.yml")
    val configuration = Configuration()
    if (!configFile.exists()) {
        Logger.warn("Config file not found, creating new one with defaults")
        configuration.saveToFile(configFile)
    }
    configuration.loadFromFile(configFile)

    Logger.debug("Starting database connection")
    DatabaseManager().start()
    Logger.debug("Starting webserver")
    WebServer().start()
}