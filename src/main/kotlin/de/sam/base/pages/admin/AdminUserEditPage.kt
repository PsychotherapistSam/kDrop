package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDTO

class AdminUserEditPage : Page(
    name = "Edit User",
    title = "Admin - Edit User",
    templateName = "user/edit.kte"
) {
    companion object {
        const val ROUTE: String = "/admin/user/edit"
    }

    var selectedUserDTO: UserDTO? = null

    override fun get() {
        pageDiff = ctx.attribute<Long>("userQueryTime") ?: 0L
        selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")
        name = "User: ${selectedUserDTO?.name}"
        title = "User: ${selectedUserDTO?.name}"
    }
}