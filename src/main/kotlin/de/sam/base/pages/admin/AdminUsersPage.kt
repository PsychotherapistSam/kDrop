package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDAO
import de.sam.base.database.UserDTO
import de.sam.base.database.UsersTable
import de.sam.base.database.toUser
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.Context
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
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

    var userDTOs = listOf<UserDTO>()
    var currenTablePage = 0
    val maxTablePageSize = 10
    var searchQuery = ""

    override fun handle(ctx: Context) {
        searchQuery = ctx.queryParam("search") ?: ""
        currenTablePage = ctx.queryParam("page")?.toInt() ?: 0

        pageDiff = measureNanoTime {
            transaction {
                logTimeSpent("Getting user list") {
                    val userData = if (searchQuery != null) {
                        UserDAO.find { UsersTable.name.lowerCase().like("%$searchQuery%".lowercase()) }
                    } else {
                        UserDAO.all()
                    }

                    userDTOs = userData
                        .orderBy(UsersTable.registrationDate to SortOrder.ASC)
                        .limit(maxTablePageSize, maxTablePageSize * currenTablePage)
                        .map { it.toUser() }
                }
            }
        }

        if (ctx.queryParam("table") != null) {
            ctx.render(
                "components/usersTable.kte",
                mapOf(
                    "users" to userDTOs,
                    "currentPage" to currenTablePage,
                    "pageSize" to maxTablePageSize,
                    "searchQuery" to searchQuery
                )
            )
            return
        }

        super.handle(ctx)
    }
}