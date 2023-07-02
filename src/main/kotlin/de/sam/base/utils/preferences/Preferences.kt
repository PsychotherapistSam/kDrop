package de.sam.base.utils.preferences

class Preferences {
    companion object {
        val preferencesList = listOf(
            Triple("dark-mode", Boolean, "Dark Mode"),
            Triple("file-previews", Boolean, "Show File Previews"),
            Triple("show-usage-quota", Boolean, "Show Usage Quota"),
        )
    }
}