package de.sam.base

import de.sam.base.config.Configuration
import de.sam.base.database.DatabaseManager
import java.io.File

fun main(args: Array<String>) {
    val configFile = File("./config.yml")
    val configuration = Configuration()
    if (!configFile.exists()) {
        configuration.saveToFile(configFile)
        println("created config yeee")
    }

    configuration.loadFromFile(configFile)

    DatabaseManager().start()
    WebServer().start()
}
