package de.sam.base.pages

import de.sam.base.Page
import de.sam.base.database.DatabaseManager
import io.javalin.http.Context
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction

class IndexPage(ctx: Context) : Page(ctx) {
    companion object {
        const val ROUTE: String = "/"
    }

    override var name: String = "Index"
    override var title: String = "Index"
    override var pageDescription: String = "Index of the homepage"
    override var templateName: String = "index.kte"

    lateinit var firstUser: DatabaseManager.User

    override fun render() {
        println("rendering $name page, overriding default render()")

        transaction {
            addLogger(StdOutSqlLogger)

            logTimeSpent("Getting first user") {
                firstUser = DatabaseManager.User.all()
                    .orderBy(DatabaseManager.UsersTable.registrationDate to SortOrder.ASC)
                    .limit(1)
                    .first()
            }
        }

        super.render()
    }
}