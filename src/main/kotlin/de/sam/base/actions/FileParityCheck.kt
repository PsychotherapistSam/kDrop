package de.sam.base.actions

import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.isValidUUID
import de.sam.base.database.FileDAO
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

class FileParityCheck {
    fun checkIfLocalFilesExistInDatabase() {
        transaction {
            val localFilesList = File(config.fileDirectory)
                .walk()
                .toList()
                .map { it.name }
                .filter { it.isValidUUID() }
                .map { UUID.fromString(it) }

            val existingFiles = FileDAO.forIds(localFilesList).map { it.id.value }

            val filesNotInDB = localFilesList.filter { !existingFiles.contains(it) }
            // get all files that do not exist in the database
//            val filesThatDoNotExistInDatabase = localFilesList.filter { FileDAO.findById(it) == null }

            filesNotInDB.forEach {
                println("File $it does not exist in the database, deleting it locally...")
                File("${config.fileDirectory}/$it").delete()
            }
        }
    }
}