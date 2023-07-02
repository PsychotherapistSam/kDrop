package de.sam.base.services

import de.sam.base.database.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class FileService {

//    fun getRootFileForUser(user: UserDTO): FileDTO? {
//        return getRootFileForUser(user.id)
//    }

    //    fun getRootFileForUser(id: UUID): FileDTO? {
    fun getRootFileForUser(user: UserDTO): FileDTO? =
        transaction {
            return@transaction FileDAO.find { FilesTable.owner eq user.id and FilesTable.isRoot }
                .firstOrNull()
                ?.toDTO()
        }

}