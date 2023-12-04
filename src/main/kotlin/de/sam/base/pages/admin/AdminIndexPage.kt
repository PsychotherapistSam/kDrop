package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.authentication.UserService
import de.sam.base.services.FileService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureNanoTime

class AdminIndexPage : Page(
    name = "Admin Overview",
    title = "Admin - Overview",
    templateName = "admin/index.kte"
), KoinComponent {
    companion object {
        const val ROUTE: String = "/admin"
    }

    private val userService: UserService by inject()
    private val fileService: FileService by inject()

    var userCount = 0
    var fileCount = 0

    override fun get() {
        pageDiff = measureNanoTime {
            userCount = userService.countTotalUsers()
            fileCount = fileService.countTotalFiles()
        }
    }
}