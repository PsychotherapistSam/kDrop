package de.sam.base.pages

import de.sam.base.Page
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import de.sam.base.utils.logging.logTimeSpent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

class SetupPage : Page(
    name = "Setup",
    templateName = "setup.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    override fun get() {
    }
}