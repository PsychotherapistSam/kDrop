package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.config.Configuration
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import de.sam.base.utils.isLoggedIn
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse

class UserRegistrationPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Register"
    override var title: String
        get() = name
        set(value) {}
    override var pageDescription: String = "User Registration"
    override var templateName: String = "user/registration.kte"

    override fun handle(ctx: Context) {
        if (ctx.isLoggedIn && ctx.currentUser!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            throw ForbiddenResponse("You are already registered.")
        } else if (!ctx.isLoggedIn && !Configuration.config.allowUserRegistration) {
            throw ForbiddenResponse("User registration is currently disabled.")
        }

        super.handle(ctx)
    }
}