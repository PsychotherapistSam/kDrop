package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.FileDAO
import de.sam.base.database.UserDAO
import de.sam.base.utils.logging.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.measureNanoTime

class AdminIndexPage : Page(
    name = "Admin Overview",
    title = "Admin - Overview",
    templateName = "admin/index.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    var userCount = 0
    var fileCount = 0

    override fun get() {
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
    }
}