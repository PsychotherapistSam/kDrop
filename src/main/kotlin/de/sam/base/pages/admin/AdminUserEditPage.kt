package de.sam.base.pages.admin

import de.sam.base.Page
import de.sam.base.database.User
import io.javalin.http.Context

class AdminUserEditPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "User"
    override var title: String = "Admin - User"
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "admin/user_edit.kte"

    var selectedUser: User? = null

    override fun handle(ctx: Context) {
        pageDiff = ctx.attribute<Long>("userQueryTime") ?: 0L
        selectedUser = ctx.attribute<User>("requestUserParameter")
        name = "User: ${selectedUser?.name}"
        title = "User: ${selectedUser?.name}"
        super.handle(ctx)
    }
}