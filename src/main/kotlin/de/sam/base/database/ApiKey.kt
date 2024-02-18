package de.sam.base.database

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.io.Serializable
import java.sql.ResultSet
import java.util.*

data class ApiKeyDTO(
    var id: UUID,
    var apiKey: String,
    var user: UUID
) : Serializable

class ApiKeyDTOMapper : RowMapper<ApiKeyDTO> {
    override fun map(rs: ResultSet, ctx: StatementContext): ApiKeyDTO {
        return ApiKeyDTO(
            id = UUID.fromString(rs.getString("id")),
            apiKey = rs.getString("api_key"),
            user = UUID.fromString(rs.getString("user_id"))
        )
    }
}