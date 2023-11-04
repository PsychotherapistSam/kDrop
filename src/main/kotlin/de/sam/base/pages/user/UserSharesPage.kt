package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.ShareDTO
import de.sam.base.services.ShareService
import de.sam.base.utils.currentUserDTO

class UserSharesPage(private val shareService: ShareService) : Page(
    name = "Shared Files",
    templateName = "user/shares.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var shares = listOf<ShareDTO>()
    override fun before() {
        shares = ArrayList()
    }

    override fun get() {
        shares = shareService.getAllSharesForUser(ctx.currentUserDTO!!.id)
    }
}