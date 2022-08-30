package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.captcha.Captcha
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.validateLoginAttempt
import de.sam.base.utils.*
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

                if (config.captcha.enabled && config.captcha.locations.contains("login")) {
                    when (config.captcha.service.lowercase()) {
                        "recaptcha" -> {
                            val captchaErrors = Captcha.validate(ctx)
                            if (captchaErrors.isNotEmpty()) {
                                //TODO: reset username field if captcha is not valid
                                lastTryUsername = ctx.formParam("username") ?: ""
                                errors.addAll(captchaErrors)
                                return@prolongAtLeast
                            }
                        }
                    }
                }

                val attempt = validateLoginAttempt(ctx.formParam("username"), ctx.formParam("password"))
                // first = user, second = errors
                if (attempt.second.isNotEmpty()) {
                    lastTryUsername = ctx.formParam("username") ?: ""
                    errors.add(attempt.second.first())
                    return@prolongAtLeast
                }

                // new session id to prevent issues with persistance when old serialization objects still exist
                ctx.req.session.invalidate()
                ctx.req.getSession(true)

                ctx.currentUserDTO = attempt.first

                if (!attempt.first?.totpSecret.isNullOrBlank()) {
                    ctx.needsToVerifyTOTP = true
                    ctx.hxRedirect(UserTOTPValidatePage.ROUTE)
                } else {
                    ctx.hxRedirect("/")
                }

                //ctx.redirect("/")
                return@prolongAtLeast
            }
        }
        super.handle(ctx)
    }
}