package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.UserDTO
import io.javalin.http.Context

class AdminUserViewPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "User"
    override var title: String = "Admin - User"
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "admin/user_view.kte"

    var selectedUserDTO: UserDTO? = null

    override fun handle(ctx: Context) {
        pageDiff = ctx.attribute<Long>("userQueryTime") ?: 0L
        selectedUserDTO = ctx.attribute<UserDTO>("requestUserParameter")
        name = "User: ${selectedUserDTO?.name}"
        title = "User: ${selectedUserDTO?.name}"
        super.handle(ctx)
    }
}