package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDTO

class AdminUserViewPage : Page(
    name = "User",
    title = "Admin - User",
    templateName = "admin/user_view.kte",
) {
    companion object {
        const val ROUTE: String = "/admin/user/view"
    }

    var selectedUserDTO: UserDTO? = null

    override fun get() {
        pageDiff = ctx.attribute<Long>("userQueryTime") ?: 0L
        selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")
        name = "User: ${selectedUserDTO?.name}"
        title = "User: ${selectedUserDTO?.name}"
    }
}