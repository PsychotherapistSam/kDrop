package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.ShareDTO
import de.sam.base.services.ShareService
import de.sam.base.utils.currentUserDTO
import org.koin.core.component.inject

class UserSharesPage() : Page(
    name = "Shared Files",
    templateName = "user/shares.kte",
) {
    companion object {
        const val ROUTE: String = "/user/shares"
    }

    private val shareService: ShareService by inject()

    var shares = listOf<ShareDTO>()

    override fun get() {
        shares = shareService.getAllSharesForUser(ctx.currentUserDTO!!.id)
    }
}