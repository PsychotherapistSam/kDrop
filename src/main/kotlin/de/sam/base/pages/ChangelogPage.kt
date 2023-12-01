package de.sam.base.pages

import de.sam.base.Page

class ChangelogPage : Page(
    name = "Changelog",
    templateName = "changelog.kte"
) {
    companion object {
        const val ROUTE: String = "/changelog"
    }

}