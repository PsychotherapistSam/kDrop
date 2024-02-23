package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.authentication.apikey.ApiKeyRepository
import de.sam.base.database.FileDTO
import de.sam.base.file.repository.FileRepository
import de.sam.base.user.integrations.IntegrationRepository
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.string.isUUID
import org.koin.core.component.inject
import java.util.*

class UserIntegrationsSettingsPage : Page(
    name = "Integrations",
    templateName = "user/settings/integrations.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/integrations"
    }

    private val apiKeyRepository: ApiKeyRepository by inject()
    private val fileRepository: FileRepository by inject()
    val integrationRepository: IntegrationRepository by inject()

    var userHasApiKeys = false

    var errors: MutableList<String> = mutableListOf()

    var shareXFolder: FileDTO? = null

    override fun before() {
        userHasApiKeys = apiKeyRepository.getApiKeysForUser(ctx.currentUserDTO!!.id).isNotEmpty()

        shareXFolder = integrationRepository.getShareXFolderForUser(ctx.currentUserDTO!!.id)
    }


    override fun post() {
        val integration = ctx.formParam("integration")

        when (integration) {
            "sharex" -> {
                val folderId = ctx.formParam("upload-folder-id")

                if (!folderId.isUUID) {
                    errors.add("Invalid folder ID")
                    return
                }

                val file = fileRepository.getFileById(UUID.fromString(folderId))

                if (file == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                    errors.add("Folder not found")
                    return
                }

                val result =
                    integrationRepository.setShareXFolderForUser(ctx.currentUserDTO!!.id, file.id)

                if (!result) {
                    errors.add("Failed to set ShareX folder")
                    return
                }
            }
        }
    }
}