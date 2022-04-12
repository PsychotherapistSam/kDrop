package de.sam.base.pages

import de.sam.base.Page
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import io.javalin.http.Context
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.measureNanoTime

class IndexPage : Page() {

    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Index"
    override var title: String = "Index"
    override var pageDescription: String = "Index of the homepage"
    override var templateName: String = "index.kte"

    lateinit var firstUserDAO: UserDAO

    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            println("rendering $name page, overriding default render()")

            transaction {
                addLogger(StdOutSqlLogger)

                logTimeSpent("Getting first user") {
                    firstUserDAO = UserDAO.all()
                        .orderBy(UsersTable.registrationDate to SortOrder.ASC)
                        .limit(1)
                        .first()
                }
            }
        }
        super.handle(ctx)
    }
}