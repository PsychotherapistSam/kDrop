package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.ShareDTO
import de.sam.base.file.share.ShareRepository
import de.sam.base.utils.currentUserDTO
import org.koin.core.component.inject

class UserSharesPage : Page(
    name = "Shared Files",
    templateName = "user/shares.kte",
) {
    companion object {
        const val ROUTE: String = "/user/shares"
    }

    private val shareRepository: ShareRepository by inject()

    var shares = listOf<ShareDTO>()

    override fun get() {
        shares = shareRepository.getAllSharesForUser(ctx.currentUserDTO!!.id) ?: emptyList()
    }
}