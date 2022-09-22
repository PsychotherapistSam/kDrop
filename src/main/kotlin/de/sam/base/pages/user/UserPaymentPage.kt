package de.sam.base.pages.user

import de.sam.base.Page

class UserPaymentPage : Page(
    name = "Payments",
    templateName = "user/payment.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }
}