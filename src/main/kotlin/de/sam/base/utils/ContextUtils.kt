package de.sam.base.utils

import de.sam.base.database.DatabaseManager
import io.javalin.http.Context

fun Context.getUser(): DatabaseManager.User? {
    if (this.req.isRequestedSessionIdValid) {
        return this.sessionAttribute<DatabaseManager.User>("user")
    }
    return null
/*
    return transaction {
        addLogger(StdOutSqlLogger)
        return@transaction DatabaseManager.User.find { DatabaseManager.Users.id eq user.id }.limit(1).firstOrNull()
    }*/
}