package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDAO
import io.javalin.http.Context
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.measureNanoTime

class AdminIndexPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Admin Overview"
    override var title: String = "Admin - Overview"
    override var pageDescription: String = name
    override var templateName: String = "admin/index.kte"

    var userCount = 0
    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            transaction {
                addLogger(StdOutSqlLogger)
                logTimeSpent("Getting user count") {
                    userCount = UserDAO.count()
                }
            }
        }
        super.handle(ctx)
    }
}