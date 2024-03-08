package de.sam.base.user.integrations

import de.sam.base.database.FileDTO
import de.sam.base.database.SqlRepository
import java.util.*

interface IntegrationRepository : SqlRepository {
    /**
     * Retrieves the ShareX folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return The ShareX folder for the user as a FileDTO object.
     */
    fun getShareXFolderForUser(userId: UUID): FileDTO?

    /**
     * Sets the ShareX folder for a specific user.
     *
     * @param userId The ID of the user.
     * @param folderId The ID of the ShareX folder.
     */
    fun setShareXFolderForUser(userId: UUID, folderId: UUID): Boolean

    /**
     * Disables the ShareX folder for a specific user.
     *
     * @param userId The ID of the user.
     */
    fun disableShareXFolderForUser(userId: UUID): Boolean

}
