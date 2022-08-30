package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.getFirstError
import de.sam.base.utils.needsToVerifyTOTP
import de.sam.base.utils.validateTOTP
import io.javalin.http.Context

class UserTOTPValidatePage : Page(
    name = "Two-factor Validation",
    templateName = "user/totp/validation.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    var error = ""

    override fun handle(ctx: Context) {
        if (!ctx.needsToVerifyTOTP) {
            ctx.redirect("/")
            return
        }

        if (ctx.method() == "POST") {
            val totp = ctx.formParamAsClass<String>("totp")
                .check({ it.length == 6 }, "Your TOTP must be 6 numbers.")
                .check({ ctx.validateTOTP(it, ctx.currentUserDTO?.totpSecret!!) }, "Your TOTP is incorrect.")
                .getFirstError()

            if (totp.second != null) {
                error = totp.second!!
            } else {
                ctx.needsToVerifyTOTP = false
                ctx.redirect("/")
            }
        }
        super.handle(ctx)
    }
}