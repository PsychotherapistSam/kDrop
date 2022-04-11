package de.sam.base.pages.user

import de.sam.base.Page
import io.javalin.http.Context

class LoginPage(ctx: Context, path: String) : Page(ctx) {
    companion object {
        lateinit var ROUTE: String
    }

    init {
        ROUTE = path
    }

    override var name: String = "Login"
    override var title: String = name
    override var pageDescription: String = "User Login"
    override var templateName: String = "user/login.kte"

}