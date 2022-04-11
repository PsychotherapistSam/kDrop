package de.sam.base.pages.user

import de.sam.base.Page
import io.javalin.http.Context

class UserLoginPage(ctx: Context) : Page(ctx) {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Login"
    override var title: String = name
    override var pageDescription: String = "User Login"
    override var templateName: String = "user/login.kte"

}