package de.sam.base.pages.user.settings

import de.sam.base.Page

class UserEditPage : Page(
    name = "Settings",
    templateName = "user/edit.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }
}