package de.sam.base.services

import de.sam.base.database.*
import de.sam.base.utils.logging.logTimeSpent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

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

    fun getFileBreadcrumb(fileID: UUID): ArrayList<FileDTO> {
        val breadcrumb = arrayListOf<FileDTO>()

        logTimeSpent("the whole query") {
            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                connection = hikariDataSource!!.connection
                preparedStatement = connection.prepareStatement(
                    """
                    WITH RECURSIVE breadcrumb AS (
                        SELECT id, name, parent, is_root, 1 as depth
                        FROM t_files
                        WHERE id = CAST(? AS uuid)
                    
                        UNION ALL
                    
                        SELECT t.id, t.name, t.parent, t.is_root, b.depth + 1
                        FROM t_files t
                        JOIN breadcrumb b ON b.parent = t.id
                    )
                    SELECT * FROM breadcrumb ORDER BY depth DESC;
                    """.trimIndent()
                )

                preparedStatement.setString(1, fileID.toString()) // set id parameter

                resultSet = preparedStatement.executeQuery()
                while (resultSet.next()) {
                    val fileDTO = FileDTO(
                        id = UUID.fromString(resultSet.getString("id")),
                        name = resultSet.getString("name"),
                        parent = resultSet.getString("parent")?.let { UUID.fromString(it) },
                        isRoot = resultSet.getBoolean("is_root")
                    )
                    breadcrumb.add(fileDTO)
                }

            } finally {
                resultSet?.close()
                preparedStatement?.close()
                connection?.close()
            }
        }

        return breadcrumb
    }

    fun getFolderContentForUser(userId: UUID, folderId: UUID): ArrayList<FileDTO> {

        val files = arrayListOf<FileDTO>()

        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            connection = hikariDataSource!!.connection
            preparedStatement = connection.prepareStatement(
                """
                SELECT * FROM t_files
                WHERE owner = CAST(? AS uuid) AND parent = CAST(? AS uuid);
                """.trimIndent()
            )

            preparedStatement.setString(1, userId.toString()) // set id parameter
            preparedStatement.setString(2, folderId.toString()) // set id parameter

            resultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                files.add(
                    FileDTO(
                        id = UUID.fromString(resultSet.getString("id")),
                        name = resultSet.getString("name"),
                        path = resultSet.getString("path"),
                        mimeType = resultSet.getString("mime_type"),
                        parent = resultSet.getString("parent")?.let { UUID.fromString(it) },
                        owner = UUID.fromString(resultSet.getString("owner")),
                        size = resultSet.getLong("size"),
                        sizeHR = resultSet.getString("size_hr"),
                        password = resultSet.getString("password"),
                        created = DateTime(resultSet.getTimestamp("created")),
                        isFolder = resultSet.getBoolean("is_folder"),
                        hash = resultSet.getString("hash"),
                        isRoot = resultSet.getBoolean("is_root")
                    )
                )
            }

        } finally {
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
        }
        return files
    }


    private fun ResultSet.getStringList(columnLabel: String): ArrayList<String> {
        val sqlArray = this.getArray(columnLabel)
        return (sqlArray.array as Array<*>).map { it.toString() } as ArrayList<String>
    }


}