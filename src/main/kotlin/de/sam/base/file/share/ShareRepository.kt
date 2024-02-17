package de.sam.base.file.share

import de.sam.base.database.ShareDTO
import de.sam.base.database.SqlRepository
import java.util.*

interface ShareRepository : SqlRepository {
    /**
     * Retrieves the list of shares for a given user.
     *
     * @param userId The unique identifier of the user.
     * @return The list of shares associated with the user.
     * @throws FileServiceException if an error occurs while fetching the shares.
     */
    fun getAllSharesForUser(userId: UUID): List<ShareDTO>?

    /**
     * Deletes all shares for a given user.
     *
     * @param userId The unique identifier of the user.
     * @throws FileServiceException if an error occurs while deleting the shares.
     */
    fun deleteAllSharesForUser(userId: UUID)

    /**
     * Retrieves the list of shares for a given file.
     *
     * @param id The unique identifier of the file.
     * @return The list of shares associated with the file.
     * @throws FileServiceException if an error occurs while fetching the shares.
     */
    fun getSharesForFile(id: UUID): List<ShareDTO>?

    /**
     * Creates a new share in the database.
     *
     * @param share the share to be created
     * @return the created share
     * @throws FileServiceException if the share creation fails
     */
    fun createShare(share: ShareDTO): ShareDTO?

    /**
     * Retrieves a share by its name.
     *
     * @param name the name of the share
     * @return the ShareDTO object representing the share with the given name, or null if not found
     * @throws FileServiceException if there is an error while fetching the share
     */
    fun getShareByName(name: String): ShareDTO?

    /**
     * Retrieves a share by its ID.
     * @param id the ID of the share
     * @return the ShareDTO object representing the share with the given ID, or null if not found
     * @throws FileServiceException if there is an error while fetching the share
     */
    fun getShareById(id: UUID): ShareDTO?

    /**
     * Deletes a share from the database.
     *
     * @param id The ID of the share to be deleted.
     * @throws FileServiceException if the share could not be deleted.
     */
    fun deleteShare(id: UUID)

    /**
     * Retrieves a list of shares for the given user.
     *
     * @param userId The ID of the user.
     * @return The list of ShareDTO objects representing the shares for the user.
     * @throws FileServiceException if an error occurs while fetching the shares.
     */
    fun getSharesForUser(userId: UUID): List<ShareDTO>?

    /**
     * Updates a share in the database.
     *
     * @param share The share to be updated.
     * @throws FileServiceException if the share could not be updated.
     */
    fun updateShare(share: ShareDTO)

    /**
     * Updates the download count of a share in the database.
     *
     * @param share The share to be updated.
     * @throws FileServiceException if the share could not be updated.
     */
    fun updateShareDownloadCount(share: ShareDTO)
}