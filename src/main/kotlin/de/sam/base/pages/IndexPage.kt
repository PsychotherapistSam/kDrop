package de.sam.base.pages

import de.sam.base.Page
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import de.sam.base.utils.logging.logTimeSpent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

class IndexPage : Page(
    name = "Index",
    templateName = "index.kte"
) {
    companion object {
        const val ROUTE: String = "/"
    }

    lateinit var firstUserDAO: UserDAO
    override fun get() {
        transaction {
            logTimeSpent("Getting first user") {
                firstUserDAO = UserDAO.all()
                    .orderBy(UsersTable.registrationDate to SortOrder.ASC)
                    .limit(1)
                    .first()
            }
        }
    }
}