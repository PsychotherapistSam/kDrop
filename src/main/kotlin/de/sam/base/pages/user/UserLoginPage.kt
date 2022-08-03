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

//                val captchaSolution =
//                    ctx.formParamAsClass<String>("frc-captcha-solution")
//                        .check({ it.isNotBlank() }, "solving the captcha is required")
//                        .get()
//
//                val client = OkHttpClient()
//
//                val request = okhttp3.Request.Builder()
//                    .url("https://api.friendlycaptcha.com/api/v1/siteverify")
//                    .post(
//                        okhttp3.FormBody.Builder()
//                            .add("solution", captchaSolution)
//                            .add("secret", "A10D4MO727BTS570TEQRRPMG6AUUT0OLNC1AG3QCK9K7MBO95VKI21KD1K")
//                            .add("sitekey", "FCMSCT79EL4FHOUU")
//                            .build()
//                    )
//                    .build()
//
//
//                val response = client.newCall(request).execute()
//                val json = response.body?.string() ?: throw InternalServerErrorResponse("no response body")
//
//                // handle response using fasterxml json
//                val mapper = ObjectMapper()
//                val jsonNode = mapper.readTree(json)
//                val success = jsonNode.get("success").asBoolean()
//                if (!success) {
//                    throw BadRequestResponse("captcha solution is invalid")
//                }

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
