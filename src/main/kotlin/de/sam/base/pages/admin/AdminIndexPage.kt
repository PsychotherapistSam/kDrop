package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.FileDAO
import de.sam.base.database.UserDAO
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.measureNanoTime

class AdminIndexPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Admin Overview"
    override var title: String = "Admin - Overview"
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "admin/index.kte"

    var userCount = 0
    var fileCount = 0
    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            transaction {
                logTimeSpent("Getting user count") {
                    userCount = UserDAO.count()
                }
                logTimeSpent("Getting file count") {
                    fileCount = FileDAO.count()
                }
            }
        }
        super.handle(ctx)
    }
}