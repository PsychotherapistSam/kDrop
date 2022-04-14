package de.sam.base.controllers

import de.sam.base.database.User
import de.sam.base.database.UserDAO
import de.sam.base.database.toUser
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.measureNanoTime

class UserController {
    fun deleteUser(ctx: Context) {
        println("after")
        val selectedUser = ctx.attribute<User>("userId")!!

        if (ctx.currentUser != selectedUser && !ctx.currentUser!!.hasRolePowerLevel(UserRoles.ADMIN)) {
            throw UnauthorizedResponse("You are not allowed to delete this user")
        }
        transaction {
            UserDAO
                .findById(selectedUser.id)!!
                .delete()
        }
    }

    fun getUserParameter(ctx: Context) {
        val userQueryTime = measureNanoTime {
            ctx.pathParamAsClass<UUID>("userId")
                .check({
                    transaction {
                        logTimeSpent("Getting user by id") {
                            val userDao = UserDAO.findById(it)
                            if (userDao != null) {
                                ctx.attribute("userId", userDao.toUser())
                                return@transaction true
                            } else {
                                return@transaction false
                            }
                        }
                    }
                }, "User ID is not valid")
                .get()
        }
        ctx.attribute("userQueryTime", userQueryTime)
    }
}