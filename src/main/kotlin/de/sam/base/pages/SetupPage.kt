package de.sam.base.pages

import de.sam.base.Page

class SetupPage : Page(
    name = "Setup", templateName = "setup.kte"
) {
    companion object {
        const val ROUTE: String = "/setup"
    }

    override fun get() {
    }
}