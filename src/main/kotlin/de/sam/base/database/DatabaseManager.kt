package de.sam.base.database


import com.zaxxer.hikari.HikariDataSource
import de.sam.base.config.Configuration.Companion.config
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.postgres.PostgresPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.jdbc.PgArray
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*

lateinit var hikariDataSource: HikariDataSource
lateinit var jdbi: Jdbi

class DatabaseManager {
    fun start() {
        hikariDataSource = HikariDataSource()
        hikariDataSource.jdbcUrl =
            "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.database}"
        hikariDataSource.username = config.database.username
        hikariDataSource.password = config.database.password

        jdbi = Jdbi.create(hikariDataSource)
        jdbi.installPlugin(PostgresPlugin())
        jdbi.registerRowMapper(FileDTOMapper())
        jdbi.registerRowMapper(UserDTOMapper())
        jdbi.registerRowMapper(ShareDTOMapper())
        jdbi.registerArrayType(UUID::class.java, "uuid")
        jdbi.registerArgument(PgArrayFactory())

        Database.connect(hikariDataSource)

        transaction {
            // print sql to std-out
            addLogger(StdOutSqlLogger)
            // create users table
            SchemaUtils.create(UsersTable)
            SchemaUtils.create(FilesTable)
            SchemaUtils.create(DownloadLogTable)
            SchemaUtils.create(SharesTable)
            SchemaUtils.create(LoginLogTable)
        }
    }
}


class PgArrayFactory : ArgumentFactory {

    override fun build(type: Type, value: Any?, config: ConfigRegistry): Optional<Argument> {
        return if (value is PgArray) Optional.of(PgArrayArgument(value)) else Optional.empty()
    }

    private class PgArrayArgument(private val array: PgArray) : Argument {
        @Throws(SQLException::class)
        override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) {
            statement.setArray(position, array)
        }
    }
}
