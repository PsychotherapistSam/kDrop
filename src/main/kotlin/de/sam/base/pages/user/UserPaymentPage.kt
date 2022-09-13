package de.sam.base.pages.user

import de.sam.base.Page
import io.javalin.http.Context

class UserPaymentPage : Page(
    name = "Payments",
    templateName = "user/payment.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    override fun handle(ctx: Context) {
        super.handle(ctx)
    }
}