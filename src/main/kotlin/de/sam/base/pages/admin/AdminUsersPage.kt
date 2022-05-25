package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.User
import de.sam.base.database.UserDAO
import de.sam.base.database.UsersTable
import de.sam.base.database.toUser
import io.javalin.http.Context
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.measureNanoTime

class AdminUsersPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Users"
    override var title: String = "Admin - Users"
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "admin/users.kte"

    var users = listOf<User>()
    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            transaction {
                addLogger(StdOutSqlLogger)
                logTimeSpent("Getting user list") {
                    users = UserDAO
                        .all()
                        .orderBy(UsersTable.registrationDate to SortOrder.ASC)
                        .limit(10, 0)
                        .map { it.toUser() }
                }
            }
        }

        if (ctx.queryParam("table") != null) {
            ctx.render("components/usersTable.kte", Collections.singletonMap("users", users))
            return
        }

        super.handle(ctx)
    }
}