package de.sam.base.pages.user

import de.sam.base.Page
import io.javalin.http.Context

class LoginPage(ctx: Context) : Page(ctx) {
    companion object {
        const val ROUTE: String = "/login"
    }

    override var name: String = "Login"
    override var title: String = name
    override var pageDescription: String = "User Login"
    override var templateName: String = "user/login.kte"

}