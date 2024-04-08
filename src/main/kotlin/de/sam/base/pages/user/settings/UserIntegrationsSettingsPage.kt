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

    var shareXFolder: FileDTO? = null

    var messages = mutableListOf<Triple<String, String, String>>()

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
                    messages.add(
                        Triple(
                            "error",
                            "Error while saving integration settings for ShareX",
                            "Invalid folder ID"
                        )
                    )
                    return
                }

                val file = fileRepository.fileCache.get(UUID.fromString(folderId))

                if (file == null || !file.isOwnedByUserId(ctx.currentUserDTO!!.id)) {
                    messages.add(
                        Triple(
                            "error",
                            "Error while saving integration settings for ShareX",
                            "Folder not found"
                        )
                    )
                    return
                }

                var result =
                    integrationRepository.setShareXFolderForUser(ctx.currentUserDTO!!.id, file.id)

                shareXFolder = file

                if (!result) {
                    messages.add(
                        Triple(
                            "error",
                            "Error while saving integration settings for ShareX",
                            "Failed to set ShareX folder"
                        )
                    )
                    return
                }

                messages.add(
                    Triple(
                        "success",
                        "Successfully saved integration settings for ShareX",
                        "ShareX folder set successfully"
                    )
                )
            }

            else -> {
                messages.add(
                    Triple(
                        "Error while saving integration settings",
                        "Failure",
                        "Invalid integration"
                    )
                )
            }
        }
    }

    override fun delete() {
        val integration = ctx.formParam("integration")

        when (integration) {
            "sharex" -> {
                val result = integrationRepository.disableShareXFolderForUser(ctx.currentUserDTO!!.id)

                if (!result) {
                    messages.add(
                        Triple(
                            "error",
                            "Error while saving integration settings for ShareX",
                            "Failed to remove ShareX folder"
                        )
                    )
                    return
                }
                shareXFolder = null
            }

            else -> {
                messages.add(
                    Triple(
                        "error",
                        "Error while saving integration settings",
                        "Invalid integration"
                    )
                )
            }
        }
    }
}