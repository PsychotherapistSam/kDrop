package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDTO
import io.javalin.http.Context

class AdminUserEditPage : Page(
    name = "Edit User",
    title = "Admin - Edit User",
    templateName = "user/edit.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    var selectedUserDTO: UserDTO? = null

    override fun handle(ctx: Context) {
        pageDiff = ctx.attribute<Long>("userQueryTime") ?: 0L
        selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")
        name = "User: ${selectedUserDTO?.name}"
        title = "User: ${selectedUserDTO?.name}"
        super.handle(ctx)
    }
}