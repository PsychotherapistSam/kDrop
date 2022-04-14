package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.User
import de.sam.base.database.UserDAO
import de.sam.base.database.toUser
import io.javalin.http.Context
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.measureNanoTime

class AdminUserViewPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "User"
    override var title: String = "Admin - User"
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "admin/user_view.kte"

    var selectedUser: User? = null

    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            ctx.pathParamAsClass<UUID>("userId")
                .check({
                    transaction {
                        logTimeSpent("Getting user by id") {
                            val userDao = UserDAO.findById(it)
                            if (userDao != null) {
                                selectedUser = userDao.toUser()
                                return@transaction true
                            } else {
                                return@transaction false
                            }
                        }
                    }
                }, "User ID is not valid")
                .get()

            name = "User: ${selectedUser?.name}"
            title = "User: ${selectedUser?.name}"
        }
        super.handle(ctx)
    }
}