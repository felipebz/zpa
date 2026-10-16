/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

/**
 * Reads Oracle dictionary metadata into an immutable [OracleMetadataResult].
 *
 * The acquisition obtains its own connection from the [DataSource], runs every dictionary query in one Oracle
 * read-only transaction (`SET TRANSACTION READ ONLY`, so all queries see one consistent dictionary state), ends the
 * transaction with a rollback and closes the connection before returning. On any failure the exception propagates,
 * the transaction is rolled back, the connection is closed and no partial result is produced. Uses only standard
 * JDBC; the Oracle driver is supplied by the caller.
 *
 * Precondition: the [DataSource] must hand out an exclusive connection that is not enlisted in a transaction managed
 * elsewhere (a container or Spring transaction manager, an XA/JTA transaction), because the acquisition starts and
 * rolls back its own transaction on it. A plain driver data source or a connection pool returning idle connections
 * satisfies this. As a guard, a connection that is not in auto-commit mode is refused before anything is executed
 * or changed on it, since it may carry the caller's uncommitted work.
 */
@ZpaExperimentalApi
object OracleMetadataAcquisition {
    private const val FETCH_SIZE = 1000
    private const val BATCH = 200

    /**
     * Acquires the requested schemas and relations.
     *
     * @throws SQLException if a dictionary query fails or times out
     * @throws OracleMetadataException if the dictionary rows cannot form valid descriptors
     * @throws IllegalStateException if the connection is not in auto-commit mode (it may belong to an external
     * transaction); nothing was executed on it
     */
    @JvmStatic
    @Throws(SQLException::class)
    fun acquire(dataSource: DataSource, request: OracleMetadataRequest): OracleMetadataResult =
        dataSource.connection.use { acquire(it, request, null) }

    /** As [acquire], with the dictionary dialect forced instead of chosen from the server version (tests only). */
    @JvmSynthetic
    internal fun acquire(dataSource: DataSource, request: OracleMetadataRequest, dialect: OracleDictionaryDialect): OracleMetadataResult =
        dataSource.connection.use { acquire(it, request, dialect) }

    private fun acquire(connection: Connection, request: OracleMetadataRequest, forced: OracleDictionaryDialect?): OracleMetadataResult {
        check(connection.autoCommit) {
            "The DataSource returned a connection that is not in auto-commit mode; metadata acquisition needs an " +
                "exclusive connection outside any external transaction"
        }
        // Decided once, from driver metadata, before any dictionary query.
        val dialect = forced ?: OracleDictionaryDialect.forMajorVersion(connection.metaData.databaseMajorVersion)
        val timeout = request.queryTimeout.seconds.toInt()
        var failure: Throwable? = null
        try {
            // In auto-commit mode no transaction is open, so SET TRANSACTION is the new transaction's first statement.
            connection.autoCommit = false
            connection.createStatement().use {
                it.queryTimeout = timeout
                it.execute("SET TRANSACTION READ ONLY")
            }
            val acquiredAt = Instant.now()
            val session = session(connection, dialect.sessionSql, timeout)
            val relations = ArrayList<RelationRow>()
            val columns = ArrayList<ColumnRow>()
            for (schema in request.schemas) {
                query(connection, dialect.schemaRelationsSql, listOf(schema, schema), timeout) { relations += relationRow(it) }
                query(connection, dialect.schemaColumnsSql, listOf(schema), timeout) { columns += columnRow(it) }
            }
            val targeted = request.relations.filter { it.schema !in request.schemas }
            for (chunk in targeted.chunked(BATCH)) {
                val pairs = chunk.flatMap { listOf(it.schema, it.name) }
                query(connection, dialect.targetedRelationsSql(chunk.size), pairs + pairs, timeout) { relations += relationRow(it) }
                query(connection, dialect.targetedColumnsSql(chunk.size), pairs, timeout) { columns += columnRow(it) }
            }
            return OracleDictionaryMapping.assemble(request, session, relations, columns, acquiredAt)
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            finish(connection, failure)
        }
    }

    /**
     * Ends the read-only transaction and restores auto-commit. Restoring is attempted even when the rollback fails;
     * secondary failures are attached to the primary one.
     */
    private fun finish(connection: Connection, failure: Throwable?) {
        var error: SQLException? = null
        try {
            connection.rollback()
        } catch (e: SQLException) {
            error = e
        }
        try {
            connection.autoCommit = true
        } catch (e: SQLException) {
            if (error == null) error = e else error.addSuppressed(e)
        }
        if (error != null) {
            if (failure != null) failure.addSuppressed(error) else throw error
        }
    }

    private fun session(connection: Connection, sql: String, timeout: Int): SessionInfo {
        var info: SessionInfo? = null
        query(connection, sql, emptyList(), timeout) {
            info = SessionInfo(it.getString(1), it.getString(2), it.getString(3) ?: "", it.getString(4) ?: "")
        }
        return info ?: throw SQLException("The session context query returned no row")
    }

    // Every relation query is paired with a column query over the same scope in the same transaction.
    private fun relationRow(rs: ResultSet) = RelationRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), columnsAcquired = true)

    private fun columnRow(rs: ResultSet) = ColumnRow(
        schema = rs.getString(1),
        relation = rs.getString(2),
        name = rs.getString(3),
        columnId = rs.getBigDecimal(4),
        dataType = rs.getString(5),
        dataTypeOwner = rs.getString(6),
        dataLength = rs.getBigDecimal(7),
        charLength = rs.getBigDecimal(8),
        charUsed = rs.getString(9),
        precision = rs.getBigDecimal(10),
        scale = rs.getBigDecimal(11)
    )

    private inline fun query(connection: Connection, sql: String, parameters: List<String>, timeout: Int, row: (ResultSet) -> Unit) {
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.queryTimeout = timeout
            statement.fetchSize = FETCH_SIZE
            parameters.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { rs -> while (rs.next()) row(rs) }
        }
    }
}
