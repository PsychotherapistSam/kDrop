package de.sam.base.utils.preferences

class Preferences {
    companion object {
        val preferencesList = listOf(
            Triple("dark-mode", Boolean, "Dark Mode"),
            Triple("file-previews", Boolean, "Show File Previews"),
        )
    }
}