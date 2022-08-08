package de.sam.base

import com.stripe.Stripe
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.DatabaseManager
import org.tinylog.kotlin.Logger
import java.io.File

fun main(args: Array<String>) {

    val configFile = File("./config.yml")
    if (!configFile.exists()) {
        Logger.warn("Config file not found, creating new one with defaults")
        config.saveToFile(configFile)
    }
    config.loadFromFile(configFile)
    org.tinylog.configuration.Configuration.set("writer.level", config.logLevel)
    Logger.info("Log Level: " + config.logLevel)
    Logger.debug("Loaded config file")


    Logger.debug("Loading Stripe API Key")
    Stripe.apiKey = "sk_test_51ItxUbFFToSW5DS8FtDXJzPBtMK5TuPMZlfPhXvWBDTWZmvYmU59RTPJ2CE1gW1nL1A9DbrLigdiq2SR0opgd7Qp00xhdB3D2j";

    Logger.debug("Starting database connection")
    DatabaseManager().start()
    Logger.debug("Starting webserver")
    WebServer().start()
    Logger.info("Started Successfully")

}