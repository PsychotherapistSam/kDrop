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
}