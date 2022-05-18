package de.sam.base.utils

import de.sam.base.users.UserRoles
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import java.util.*

class CustomAccessManager : AccessManager {
    override fun manage(handler: Handler, ctx: Context, routeRoles: MutableSet<RouteRole>) {
        if (routeRoles.isNotEmpty()) {
            if (!ctx.isLoggedIn) {
                throw UnauthorizedResponse("You need to be logged in to access this resource.")
            }

            val maxUserRole = ctx.currentUser!!.roles.maxOf { it.powerLevel }
            val minReqiredRole = routeRoles
                .map { it as UserRoles }
                //.filter { !it.hidden }
                .minOf { it.powerLevel }

            val reachesRoleRequirement = maxUserRole >= minReqiredRole

            if (routeRoles.contains(UserRoles.SELF) && ctx.pathParam("userId") != null) {
                if (ctx.currentUser!!.id != UUID.fromString(ctx.pathParam("userId")) && !reachesRoleRequirement) {
                    // you can't access other users' resources if "self" is set
                    throw UnauthorizedResponse(
                        "You are not authorized to access this resource."
                    )
                }
            }

            if (!reachesRoleRequirement) {
                val minRole = routeRoles.map { it as UserRoles }.minByOrNull { it.powerLevel }
                // val minRoleName = (routeRoles.map { it as UserRoles }).minByOrNull { it.powerLevel }!!.name

                throw UnauthorizedResponse(
                    "You are not authorized to access this resource.",
                    hashMapOf("minimumRole" to minRole!!.name)
                ) //You need to be at least $minRole")
            }
/*                    // check if ctx.currentUser.roles has any role in routeRoles
                    if (!routeRoles.any { ctx.currentUser!!.roles.contains(it) }) {
                        throw UnauthorizedResponse("You are not authorized to access this resource")
                    }*/
        }
        handler.handle(ctx)
    }
}