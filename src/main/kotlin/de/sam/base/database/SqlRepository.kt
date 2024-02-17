package de.sam.base.database

import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.tinylog.kotlin.Logger
import java.sql.SQLException

interface SqlRepository {
    fun <T> executeWithExceptionHandling(messageType: String? = null, block: () -> T): T? {
        return try {
            block()
        } catch (e: UnableToExecuteStatementException) {
            Logger.error("Unable to execute statement", e)
            null
        } catch (e: SQLException) {
            Logger.error("Database error ${messageType?.let { "while $it" }}", e)
            null
        } catch (e: Exception) {
            Logger.error("Unexpected error ${messageType?.let { "while $it" }}", e)
            null
        }
    }
}