package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.config.Configuration
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse

class UserRegistrationPage : Page(
    name = "Registration",
    templateName = "user/registration.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    override fun handle(ctx: Context) {
        if (ctx.isLoggedIn && ctx.currentUserDTO!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            throw ForbiddenResponse("You are already registered.")
        } else if (!ctx.isLoggedIn && !Configuration.config.allowUserRegistration) {
            throw ForbiddenResponse("User registration is currently disabled.")
        }
        super.handle(ctx)
    }
}