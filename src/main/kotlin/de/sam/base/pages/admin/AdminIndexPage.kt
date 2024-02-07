package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.user.repository.UserRepository
import de.sam.base.file.repository.FileRepository
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

    private val userRepository: UserRepository by inject()
    private val fileRepository: FileRepository by inject()

    var userCount = 0
    var fileCount = 0

    override fun get() {
        pageDiff = measureNanoTime {
            userCount = userRepository.countTotalUsers()
            fileCount = fileRepository.countTotalFiles()
        }
    }
}