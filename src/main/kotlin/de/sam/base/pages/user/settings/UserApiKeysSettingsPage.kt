package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.authentication.apikey.ApiKeyRepository
import de.sam.base.database.ApiKeyDTO
import de.sam.base.utils.currentUserDTO
import io.javalin.http.BadRequestResponse
import io.javalin.http.InternalServerErrorResponse
import org.koin.core.component.inject

class UserApiKeysSettingsPage : Page(
    name = "API Keys",
    templateName = "user/settings/api_keys.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/apiKeys"
    }

    private val apiKeyRepository: ApiKeyRepository by inject()

    var apiKeys = emptyList<ApiKeyDTO>()

    override fun get() {
        apiKeys = apiKeyRepository.getApiKeysForUser(ctx.currentUserDTO!!.id)
    }

    override fun post() {
        val apiKey = ApiKeyDTO(
            id = java.util.UUID.randomUUID(),
            apiKey = java.util.UUID.randomUUID().toString(),
            user = ctx.currentUserDTO!!.id,
            createdAt = org.joda.time.DateTime.now()
        )

        apiKeyRepository.createApiKey(apiKey)
            ?: throw InternalServerErrorResponse("Failed to create API key.")

        apiKeys = apiKeyRepository.getApiKeysForUser(ctx.currentUserDTO!!.id)
    }

    override fun delete() {
        val apiKeyId = java.util.UUID.fromString(ctx.formParam("apiKeyId"))

        val apiKey = apiKeyRepository.getApiKeyById(apiKeyId)
            ?: throw BadRequestResponse("API key not found.")

        if (apiKey.user != ctx.currentUserDTO!!.id) {
            throw BadRequestResponse("API key not found.")
        }

        apiKeyRepository.deleteApiKey(apiKeyId)

        apiKeys = apiKeyRepository.getApiKeysForUser(ctx.currentUserDTO!!.id)
    }
}