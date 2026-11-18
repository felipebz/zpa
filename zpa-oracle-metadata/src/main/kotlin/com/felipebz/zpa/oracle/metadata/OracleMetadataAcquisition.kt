//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.squid.DatabaseVersion
import java.sql.*
import java.time.Duration
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
     * Acquires the requested schemas (relations, columns, object names and synonyms), relations and public synonyms.
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
            val version = databaseVersion(connection, timeout)
            val relations = ArrayList<RelationRow>()
            val columns = ArrayList<ColumnRow>()
            val objects = ArrayList<ObjectRow>()
            val synonyms = ArrayList<SynonymRow>()
            for (schema in request.schemas) {
                query(connection, dialect.schemaRelationsSql, listOf(schema, schema), timeout) { relations += relationRow(it) }
                query(connection, dialect.schemaColumnsSql, listOf(schema), timeout) { columns += columnRow(it) }
                query(connection, dialect.schemaObjectsSql, listOf(schema), timeout) { objects += ObjectRow(it.getString(1), it.getString(2), it.getString(3)) }
                query(connection, dialect.schemaSynonymsSql, listOf(schema), timeout) { synonyms += synonymRow(it) }
            }
            val targeted = request.relations.filter { it.schema !in request.schemas }
            for (chunk in targeted.chunked(BATCH)) {
                val pairs = chunk.flatMap { listOf(it.schema, it.name) }
                query(connection, dialect.targetedRelationsSql(chunk.size), pairs + pairs, timeout) { relations += relationRow(it) }
                query(connection, dialect.targetedColumnsSql(chunk.size), pairs, timeout) { columns += columnRow(it) }
            }
            if (request.publicSynonyms && request.publicSynonymTargets.isEmpty()) {
                query(connection, dialect.publicSynonymsSql, emptyList(), timeout) { synonyms += synonymRow(it) }
            }
            for (chunk in request.publicSynonymTargets.chunked(BATCH)) {
                query(connection, dialect.targetedPublicSynonymsSql(chunk.size), chunk, timeout) { synonyms += synonymRow(it) }
            }
            val typesStarted = System.nanoTime()
            val types = objectTypes(connection, dialect, request, synonyms, timeout)
            val typesReadIn = Duration.ofNanos(System.nanoTime() - typesStarted)
            return OracleDictionaryMapping.assemble(request, session, relations, columns, acquiredAt, version, objects, synonyms, types, typesReadIn)
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            finish(connection, failure)
        }
    }

    /**
     * The object types the metadata needs: those of the requested schemas, those the synonyms of these schemas and the
     * public synonyms stand for (a chain of the synonyms read followed to its end), and the supertypes of each, up to a
     * type that is under none. Each type is read once, with the attributes and methods it declares.
     */
    private fun objectTypes(
        connection: Connection,
        dialect: OracleDictionaryDialect,
        request: OracleMetadataRequest,
        synonyms: List<SynonymRow>,
        timeout: Int
    ): TypeRows {
        val reading = TypeReading(connection, dialect, timeout)
        for (schema in request.schemas) {
            reading.read(OracleTypeScope.SCHEMA, listOf(schema))
            reading.read(OracleTypeScope.SCHEMA_SYNONYMS, listOf(schema))
        }
        if (request.publicSynonyms && request.publicSynonymTargets.isEmpty()) reading.read(OracleTypeScope.PUBLIC_SYNONYMS, emptyList())
        for (chunk in request.publicSynonymTargets.chunked(BATCH)) reading.read(OracleTypeScope.publicSynonymsOf(chunk.size), chunk)
        val tried = HashSet<Pair<String, String>>()
        while (true) {
            val missing = (reading.supertypes() + synonymChainEnds(synonyms)).filter { it !in reading.keys && tried.add(it) }
            if (missing.isEmpty()) return reading.rows()
            for (chunk in missing.chunked(BATCH)) reading.read(OracleTypeScope.named(chunk.size), chunk.flatMap { listOf(it.first, it.second) })
        }
    }

    /** Where each local synonym read ends once followed through the other synonyms read, when it stands for another one. */
    private fun synonymChainEnds(synonyms: List<SynonymRow>): List<Pair<String, String>> {
        val byName = synonyms.filter { it.dbLink == null }.associateBy { it.owner to it.name }
        return byName.values.mapNotNull { chainEnd(it, byName) }
    }

    private fun chainEnd(synonym: SynonymRow, byName: Map<Pair<String, String>, SynonymRow>): Pair<String, String>? {
        val seen = hashSetOf(synonym.owner to synonym.name)
        var target = targetOf(synonym) ?: return null
        var hops = 0
        while (true) {
            val next = byName[target] ?: return target.takeIf { hops > 0 }
            // Oracle rejects a synonym that resolves to itself (ORA-01775).
            if (!seen.add(target)) return null
            target = targetOf(next) ?: return null
            hops++
        }
    }

    private fun targetOf(synonym: SynonymRow): Pair<String, String>? =
        synonym.targetOwner?.let { owner -> synonym.targetName?.let { owner to it } }

    /** The rows of the object types read so far, each type from the first query that returned it. */
    private class TypeReading(private val connection: Connection, private val dialect: OracleDictionaryDialect, private val timeout: Int) {
        private val types = ArrayList<TypeRow>()
        private val attributes = ArrayList<TypeAttributeRow>()
        private val methods = ArrayList<TypeMethodRow>()
        private val parameters = ArrayList<MethodParameterRow>()
        private val results = ArrayList<MethodResultRow>()
        val keys = HashSet<Pair<String, String>>()

        fun supertypes(): List<Pair<String, String>> = types.mapNotNull { row ->
            row.supertypeOwner?.let { owner -> row.supertypeName?.let { owner to it } }
        }

        fun rows() = TypeRows(types, attributes, methods, parameters, results)

        fun read(scope: OracleTypeScope, parameters: List<String>) {
            val found = ArrayList<TypeRow>()
            query(connection, dialect.typesSql(scope), parameters, timeout) {
                found += TypeRow(it.getString(1), it.getString(2), it.getString(3), it.getString(4), it.getString(5))
            }
            val fresh = found.filter { (it.owner to it.name) !in keys }
            if (fresh.isEmpty()) return
            val wanted = fresh.mapTo(HashSet()) { it.owner to it.name }
            types += fresh
            keys += wanted
            val inScope = { owner: String, type: String -> (owner to type) in wanted }
            query(connection, dialect.typeAttributesSql(scope), parameters, timeout) {
                if (inScope(it.getString(1), it.getString(2))) attributes += TypeAttributeRow(
                    it.getString(1), it.getString(2), it.getString(3), it.getString(4), it.getString(5), it.getString(6),
                    it.getBigDecimal(7), it.getBigDecimal(8), it.getBigDecimal(9), it.getString(10)
                )
            }
            val declared = HashSet<List<String>>()
            query(connection, dialect.typeMethodsSql(scope), parameters, timeout) {
                if (inScope(it.getString(1), it.getString(2))) {
                    val row = TypeMethodRow(it.getString(1), it.getString(2), it.getString(3), it.getBigDecimal(4), it.getBigDecimal(5))
                    methods += row
                    declared += listOf(row.owner, row.type, row.number.toPlainString())
                }
            }
            // A method's parameters and result are kept only for a method the type declares itself.
            val method = { rs: ResultSet -> listOf(rs.getString(1), rs.getString(2), rs.getBigDecimal(4).toPlainString()) in declared }
            query(connection, dialect.methodParametersSql(scope), parameters, timeout) {
                if (method(it)) this.parameters += MethodParameterRow(
                    it.getString(1), it.getString(2), it.getString(3), it.getBigDecimal(4), it.getString(5), it.getString(6), it.getString(7),
                    it.getString(8), it.getString(9)
                )
            }
            query(connection, dialect.methodResultsSql(scope), parameters, timeout) {
                if (method(it)) results += MethodResultRow(
                    it.getString(1), it.getString(2), it.getString(3), it.getBigDecimal(4), it.getString(5), it.getString(6), it.getString(7)
                )
            }
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

    private fun databaseVersion(connection: Connection, timeout: Int): Result<DatabaseVersion> = try {
        connection.prepareCall("BEGIN ? := DBMS_DB_VERSION.VERSION; ? := DBMS_DB_VERSION.RELEASE; END;").use {
            it.queryTimeout = timeout
            it.registerOutParameter(1, Types.INTEGER)
            it.registerOutParameter(2, Types.INTEGER)
            it.execute()
            Result.success(DatabaseVersion(it.getInt(1), it.getInt(2)))
        }
    } catch (e: SQLException) {
        Result.failure(e)
    }

    private fun session(connection: Connection, sql: String, timeout: Int): SessionInfo {
        var info: SessionInfo? = null
        query(connection, sql, emptyList(), timeout) {
            info = SessionInfo(it.getString(1), it.getString(2), it.getString(3) ?: "", it.getString(4) ?: "")
        }
        return info ?: throw SQLException("The session context query returned no row")
    }

    // Every relation query is paired with a column query over the same scope in the same transaction.
    private fun relationRow(rs: ResultSet): RelationRow {
        val view = rs.getString(5) == "VIEW"
        return RelationRow(
            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), columnsAcquired = true, view = view,
            valid = !view || rs.getString(6) == "VALID"
        )
    }

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
        scale = rs.getBigDecimal(11),
        nullable = rs.getString(12)
    )

    private fun synonymRow(rs: ResultSet) = SynonymRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5))

    private inline fun query(connection: Connection, sql: String, parameters: List<String>, timeout: Int, row: (ResultSet) -> Unit) {
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.queryTimeout = timeout
            statement.fetchSize = FETCH_SIZE
            parameters.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { rs -> while (rs.next()) row(rs) }
        }
    }
}
