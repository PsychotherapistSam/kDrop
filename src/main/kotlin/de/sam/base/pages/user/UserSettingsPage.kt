package de.sam.base.pages.user

import de.sam.base.Page

class UserSettingsPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Settings"
    override var title: String
        get() = name
        set(value) {}
    override var pageDescription: String = "User Settings"
    override var templateName: String = "user/settings.kte"
}