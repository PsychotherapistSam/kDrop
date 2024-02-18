package de.sam.base.authentication.apikey

import de.sam.base.database.ApiKeyDTO
import de.sam.base.database.SqlRepository
import de.sam.base.database.UserDTO
import java.util.*

interface ApiKeyRepository : SqlRepository {
    /**
     * Retrieves an [ApiKeyDTO] by its ID.
     *
     * @param id The ID of the API key to retrieve.
     * @return The [ApiKeyDTO] corresponding to the given ID, or null if no API key is found.
     */
    fun getApiKeyById(id: UUID): ApiKeyDTO?

    /**
     * Retrieves an [ApiKeyDTO] by its API key.
     *
     * @param apiKey The API key to retrieve.
     * @return The [ApiKeyDTO] corresponding to the given API key, or null if no API key is found.
     */
    fun getApiKeyByApiKey(apiKey: String): ApiKeyDTO?

    /**
     * Retrieves the [UserDTO] associated with the provided API key.
     *
     * @param apiKey The API key to retrieve the user for.
     * @return The [UserDTO] object corresponding to the given API key, or null if no user is found.
     */
    fun getUserForApiKey(apiKey: String): UserDTO?

    /**
     * Retrieves all API keys associated with a user by their user ID.
     *
     * @param userId The ID of the user.
     * @return A list of [ApiKeyDTO] objects representing the API keys associated with the user.
     */
    fun getApiKeysForUser(userId: UUID): List<ApiKeyDTO>

    /**
     * Creates a new API key based on the provided [ApiKeyDTO].
     *
     * @param apiKey The API key details, including ID, API key string, associated user, and creation timestamp.
     * @return The generated UUID for the newly created API key.
     */
    fun createApiKey(apiKey: ApiKeyDTO): ApiKeyDTO?

    /**
     * Deletes an API key with the given ID.
     *
     * @param apiKeyId The ID of the API key to delete.
     * @return True if the API key is successfully deleted, false otherwise.
     */
    fun deleteApiKey(apiKeyId: UUID?)

}