package de.sam.base.pages.user

import de.sam.base.Page
import io.javalin.http.Context

class UserSettingsPage(ctx: Context) : Page(ctx) {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Settings"
    override var title: String = name
    override var pageDescription: String = "User Settings"
    override var templateName: String = "user/settings.kte"
}