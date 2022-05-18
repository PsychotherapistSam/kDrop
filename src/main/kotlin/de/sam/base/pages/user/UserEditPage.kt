package de.sam.base.pages.user

import de.sam.base.Page

class UserEditPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "Settings"
    override var title: String = name
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "user/edit.kte"

}