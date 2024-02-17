package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDTO
import de.sam.base.user.UserRepository
import de.sam.base.utils.logging.logTimeSpent
import org.koin.core.component.inject
import kotlin.system.measureNanoTime

class AdminUsersPage : Page(
    name = "Users",
    title = "Admin - Users",
    templateName = "admin/users.kte",
) {
    companion object {
        const val ROUTE: String = "/admin/users"
    }

    private val userRepository: UserRepository by inject()

    var userDTOs = listOf<UserDTO>()
    var currenTablePage = 0
    val maxTablePageSize = 10
    var searchQuery = ""

    override fun get() {
        searchQuery = ctx.queryParam("search") ?: ""
        currenTablePage = ctx.queryParam("page")?.toInt() ?: 0

        pageDiff = measureNanoTime {
            logTimeSpent("Getting user list") {
                userDTOs =
                    if (searchQuery.isNotBlank())
                        userRepository.searchUsers(searchQuery, maxTablePageSize, maxTablePageSize * currenTablePage)
                    else
                        userRepository.getAllUsers(maxTablePageSize, maxTablePageSize * currenTablePage)
            }
        }

        if (ctx.queryParam("table") != null) {
            renderTemplate = false
            ctx.render(
                "components/usersTable.kte",
                mapOf(
                    "users" to userDTOs,
                    "currentPage" to currenTablePage,
                    "pageSize" to maxTablePageSize,
                    "searchQuery" to searchQuery
                )
            )
            return
        }
    }
}