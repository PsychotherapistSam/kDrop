package de.sam.base.database


import com.zaxxer.hikari.HikariDataSource
import de.sam.base.config.Configuration
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.statement.SqlLogger
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.postgres.PostgresPlugin
import org.postgresql.jdbc.PgArray
import org.tinylog.kotlin.Logger
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.temporal.ChronoUnit
import java.util.*


lateinit var hikariDataSource: HikariDataSource
lateinit var jdbi: Jdbi

class DatabaseManager(private val config: Configuration) {

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
        jdbi.registerRowMapper(LoginLogDTOMapper())
        jdbi.registerRowMapper(ApiKeyDTOMapper())
        jdbi.registerArrayType(UUID::class.java, "uuid")
        jdbi.registerArgument(PgArrayFactory())

        val sqlLogger: SqlLogger = object : SqlLogger {
            override fun logAfterExecution(context: StatementContext) {
                val sqlWithoutParameters = context.rawSql
                    .replace("\n", " ")
                    .replace(Regex("\\s+"), " ")

                Logger.tag("Query").info(
                    "Executed SQL: [{}], Execution Time: {} ms",
                    sqlWithoutParameters, context.getElapsedTime(ChronoUnit.MILLIS)
                )
            }
        }
        jdbi.setSqlLogger(sqlLogger)

        val flyway = Flyway.configure().dataSource(hikariDataSource).load()
        flyway.migrate()
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
