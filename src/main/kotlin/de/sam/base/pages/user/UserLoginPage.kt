package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.controllers.validateLoginAttempt
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.prolongAtLeast
import io.javalin.http.Context

class UserLoginPage : Page(
    name = "Login",
    templateName = "user/login.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var lastTryUsername: String = ""
    var errors: MutableList<String> = mutableListOf()

    override fun handle(ctx: Context) {
        errors.clear()
        lastTryUsername = ""

        if (ctx.method() == "POST") {
            prolongAtLeast(2000) {
                if (ctx.isLoggedIn) {
                    ctx.hxRedirect("/")
                    return@prolongAtLeast
                }

                val attempt = validateLoginAttempt(ctx.formParam("username"), ctx.formParam("password"))
                // first = user, second = errors
                if (attempt.second.isNotEmpty()) {
                    lastTryUsername = ctx.formParam("username") ?: ""
                    errors.add(attempt.second.first())
                    return@prolongAtLeast
                }

                ctx.currentUserDTO = attempt.first
                ctx.hxRedirect("/")
                //ctx.redirect("/")
                return@prolongAtLeast
            }
        }
        super.handle(ctx)
    }
}

//TODO: fix actual redirecting, this doesnt seem to work.
private fun Context.hxRedirect(route: String) {
    this.redirect(route)
    this.header("HX-Push", route)
    /*HX-Push - pushes a new URL into the browser’s address bar
    HX-Redirect*/
}
