package de.sam.base

import de.sam.base.database.DatabaseManager

fun main(args: Array<String>) {
    DatabaseManager().start()
    WebServer().start()
}
