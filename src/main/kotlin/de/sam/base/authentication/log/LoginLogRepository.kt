package de.sam.base.authentication.log

import de.sam.base.database.LoginLogDTO
import de.sam.base.database.SqlRepository
import io.javalin.http.Context
import org.joda.time.DateTime
import java.util.*

interface LoginLogRepository : SqlRepository {
    /**
     * Logs a login attempt.
     *
     * @param ctx The context of the request.
     * @param userId The unique identifier of the user.
     * @throws Exception if an error occurs while logging the login.
     */
    fun logLoginForUserId(ctx: Context, userId: UUID, date: DateTime)

    /**
     * Retrieves the login history for a given user.
     *
     * @param userId The unique identifier of the user.
     * @return The list of login logs associated with the user.
     * @throws Exception if an error occurs while fetching the login logs.
     */
    fun getLoginHistoryByUserId(userId: UUID): List<LoginLogDTO>?

    /**
     * Retrieves the limited login history for a given user within a specified number of days.
     *
     * @param userId The unique identifier of the user.
     * @param days The number of days to limit the login history.
     * @return The list of limited login logs associated with the user.
     * @throws Exception if an error occurs while fetching the limited login logs.
     */
    fun getLimitedLoginHistoryByUserId(userId: UUID, days: Int): List<LoginLogDTO>?

    /**
     * Deletes all login logs for a given user.
     *
     * @param userId The unique identifier of the user.
     * @throws Exception if an error occurs while deleting the login logs.
     */
    fun deleteAllLoginLogsForUser(userId: UUID)

    /**
     * Retrieves the login log for a given session.
     *
     * @param sessionId The unique identifier of the session.
     * @return The login log associated with the session.
     * @throws Exception if an error occurs while fetching the login log.
     */
    fun getLogBySessionId(sessionId: String): LoginLogDTO?

    /**
     * Retrieves the login log for a given id.
     *
     * @param id The unique identifier of the login log.
     * @return The login log associated with the id.
     * @throws Exception if an error occurs while fetching the login log.
     */
    fun getLogById(id: UUID): LoginLogDTO?

    /**
     * Deletes a login log entry.
     *
     * @param id The unique identifier of the login log.
     * @throws Exception if an error occurs while deleting the login log.
     */
    fun removeLogEntry(id: UUID)

    /**
     * Updates a login log entry.
     *
     * @param loginLog The login log to update.
     * @throws Exception if an error occurs while updating the login log.
     */
    fun updateLoginLogEntry(loginLog: LoginLogDTO)
}