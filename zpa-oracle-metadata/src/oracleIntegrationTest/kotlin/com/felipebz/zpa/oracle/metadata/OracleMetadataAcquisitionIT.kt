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
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/** Acquisition against a real Oracle database, compared with what Oracle itself compiles and selects. */
@OptIn(ZpaExperimentalApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleMetadataAcquisitionIT {
    private lateinit var db: OracleDatabase
    private lateinit var owner: OracleDatabase.User
    private lateinit var reader: OracleDatabase.User
    private lateinit var full: OracleMetadataResult

    private val pkg by lazy {
        """
        CREATE OR REPLACE PACKAGE pkg_meta AS
          g_name test_table.name%TYPE;
          g_row test_table%ROWTYPE;
          g_copy g_name%TYPE;
          g_q ${owner.name}.test_table.id%TYPE;
          g_mixed "MixedCase"."MixedCol"%TYPE;
          FUNCTION take(p test_table.born%TYPE) RETURN VARCHAR2;
          FUNCTION take(p test_table.name%TYPE) RETURN VARCHAR2;
        END pkg_meta;
        """.trimIndent()
    }

    @BeforeAll
    fun setUp() {
        db = OracleDatabase.connect()
        owner = db.createUser("ZPAMD_OWNER", "CREATE TABLE", "CREATE VIEW", "CREATE TYPE", "CREATE PROCEDURE")
        reader = db.createUser("ZPAMD_READER")
        owner.execute(
            "CREATE TABLE test_table (id NUMBER PRIMARY KEY, name VARCHAR2(30 BYTE), label VARCHAR2(20 CHAR), born DATE, " +
                "created TIMESTAMP, amount NUMBER(10,2), neg NUMBER(5,-2), fl FLOAT(10), secret VARCHAR2(10) INVISIBLE, note VARCHAR2(100))",
            "CREATE VIEW test_view AS SELECT id, name full_name, amount * 2 dbl, CAST(label AS VARCHAR2(5)) lbl FROM test_table",
            "CREATE TABLE inv_order (a NUMBER, b NUMBER INVISIBLE, c NUMBER)",
            "ALTER TABLE inv_order MODIFY (b VISIBLE)",
            "CREATE TYPE addr_t AS OBJECT (street VARCHAR2(30), city VARCHAR2(30))",
            "CREATE TABLE dt_table (x XMLTYPE, a addr_t, j JSON, r RAW(16), n NCHAR(4), xs SYS.XMLTYPE)",
            "CREATE TABLE addr_tab OF addr_t",
            "CREATE TABLE \"MixedCase\" (\"MixedCol\" NUMBER, plain DATE)",
            "GRANT SELECT ON test_table TO ${reader.name}",
            pkg,
            """
            CREATE OR REPLACE PACKAGE BODY pkg_meta AS
              FUNCTION take(p test_table.born%TYPE) RETURN VARCHAR2 IS BEGIN RETURN 'born'; END;
              FUNCTION take(p test_table.name%TYPE) RETURN VARCHAR2 IS BEGIN RETURN 'name'; END;
            END pkg_meta;
            """.trimIndent()
        )
        full = OracleMetadataAcquisition.acquire(owner.dataSource, OracleMetadataRequest.builder().schema(owner.name).build())
    }

    @AfterAll
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun id(s: String) = ProjectIdentifier.fromSource("\"$s\"")
    private val schema get() = id(owner.name)
    private fun relation(name: String, result: OracleMetadataResult = full) = result.metadata.findRelation(schema, id(name)).relation.get()
    private fun names(columns: List<ProjectExternalColumn>) = columns.map { it.name.originalSpelling.trim('"') }
    private val empty = ProjectSnapshots.prepare(emptyList(), false)
    private fun written(kind: Anchor, vararg name: String, metadata: ProjectExternalMetadata = full.metadata) =
        empty.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of(*name), kind), null, metadata, schema)

    @Test
    fun ordinaryTableColumnsAndDatatypes() {
        val t = relation("TEST_TABLE")
        assertThat(t.isColumnsComplete).isTrue()
        assertThat(names(t.columns)).containsExactly("ID", "NAME", "LABEL", "BORN", "CREATED", "AMOUNT", "NEG", "FL", "NOTE", "SECRET")
        fun type(c: String) = t.columns.single { it.name == id(c) }.datatype.get()
        assertThat(type("ID").precision).isEqualTo(OptionalInt.empty())
        assertThat(type("ID").scale).isEqualTo(OptionalInt.empty())
        assertThat(type("NAME").length).isEqualTo(OptionalInt.of(30))
        assertThat(type("NAME").lengthSemantics).contains(ProjectExternalDatatype.LengthSemantics.BYTE)
        assertThat(type("LABEL").length).isEqualTo(OptionalInt.of(20))
        assertThat(type("LABEL").lengthSemantics).contains(ProjectExternalDatatype.LengthSemantics.CHAR)
        assertThat(type("AMOUNT").precision).isEqualTo(OptionalInt.of(10))
        assertThat(type("AMOUNT").scale).isEqualTo(OptionalInt.of(2))
        assertThat(type("NEG").scale).isEqualTo(OptionalInt.of(-2))
        assertThat(type("FL").name).isEqualTo("FLOAT")
        assertThat(type("FL").precision).isEqualTo(OptionalInt.of(10))
        assertThat(type("CREATED").name).isEqualTo("TIMESTAMP(6)")
        assertThat(type("BORN").length).isEqualTo(OptionalInt.empty())
        assertThat(full.columnCount).isGreaterThanOrEqualTo(t.columns.size)
    }

    @Test
    fun viewsWithRenamedAndComputedColumns() {
        val v = relation("TEST_VIEW")
        assertThat(names(v.rowFields)).containsExactly("ID", "FULL_NAME", "DBL", "LBL")
        val lbl = v.columns.single { it.name == id("LBL") }.datatype.get()
        assertThat(lbl.length).isEqualTo(OptionalInt.of(5))
        assertThat(written(Anchor.TYPE, "test_view", "full_name").status).isEqualTo(Status.RESOLVED)
        assertThat(written(Anchor.TYPE, "test_view", "name").status).isEqualTo(Status.COLUMN_NOT_FOUND)
        assertThat(owner.accepts("DECLARE v test_view.full_name%TYPE; BEGIN NULL; END;")).isTrue()
        assertThat(owner.accepts("DECLARE v test_view.name%TYPE; BEGIN NULL; END;")).isFalse()
    }

    @Test
    fun invisibleColumnsAndRowFieldOrder() {
        val t = relation("TEST_TABLE")
        assertThat(t.columns.single { it.name == id("SECRET") }.position).isEqualTo(OptionalInt.empty())
        assertThat(names(t.rowFields)).doesNotContain("SECRET")
        assertThat(written(Anchor.TYPE, "test_table", "secret").status).isEqualTo(Status.RESOLVED)
        assertThat(owner.accepts("DECLARE v test_table.secret%TYPE; BEGIN NULL; END;")).isTrue()
        assertThat(owner.accepts("DECLARE r test_table%ROWTYPE; BEGIN r.secret := 'x'; END;")).isFalse()
        assertThat(names(relation("INV_ORDER").rowFields)).containsExactly("A", "C", "B")
        // Oracle assigns SELECT * positionally in COLUMN_ID order: the third value lands in B.
        assertThat(owner.call("DECLARE r inv_order%ROWTYPE; BEGIN SELECT 1, 2, 3 INTO r FROM dual; ? := r.a || r.b || r.c; END;")).isEqualTo("132")
    }

    @Test
    fun systemGeneratedHiddenColumnsAreExcluded() {
        val dt = relation("DT_TABLE")
        assertThat(names(dt.columns)).containsExactly("X", "A", "J", "R", "N", "XS")
        assertThat(dt.columns.map { it.position.asInt }).containsExactly(1, 2, 3, 4, 5, 6)
        val xml = dt.columns.single { it.name == id("X") }.datatype.get()
        assertThat(xml.name).isEqualTo("XMLTYPE")
        // DATA_TYPE_OWNER is kept verbatim: it records how the column was declared (PUBLIC synonym or SYS).
        assertThat(xml.typeOwner).contains(id("PUBLIC"))
        assertThat(dt.columns.single { it.name == id("XS") }.datatype.get().typeOwner).contains(id("SYS"))
        assertThat(dt.columns.single { it.name == id("A") }.datatype.get().typeOwner).contains(schema)
        assertThat(dt.columns.single { it.name == id("N") }.datatype.get().lengthSemantics).contains(ProjectExternalDatatype.LengthSemantics.CHAR)
        assertThat(dt.columns.single { it.name == id("R") }.datatype.get().length).isEqualTo(OptionalInt.of(16))
    }

    @Test
    fun objectTablesKeepTheObjectRowType() {
        val tab = relation("ADDR_TAB")
        assertThat(tab.rowObjectType.get().name).isEqualTo("ADDR_T")
        assertThat(tab.rowObjectType.get().typeOwner).contains(schema)
        assertThat(names(tab.rowFields)).containsExactly("STREET", "CITY")
        assertThat(written(Anchor.ROWTYPE, "addr_tab").status).isEqualTo(Status.RESOLVED)
        assertThat(owner.accepts("DECLARE v addr_tab%ROWTYPE; BEGIN v := addr_t('s', 'c'); END;")).isTrue()
    }

    @Test
    fun quotedMixedCaseIdentifiers() {
        assertThat(written(Anchor.TYPE, "\"MixedCase\"", "\"MixedCol\"").status).isEqualTo(Status.RESOLVED)
        assertThat(written(Anchor.TYPE, "\"MixedCase\"", "plain").status).isEqualTo(Status.RESOLVED)
        assertThat(written(Anchor.TYPE, "mixedcase", "mixedcol").status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(owner.accepts("DECLARE v \"MixedCase\".\"MixedCol\"%TYPE; BEGIN NULL; END;")).isTrue()
        assertThat(owner.accepts("DECLARE v mixedcase.mixedcol%TYPE; BEGIN NULL; END;")).isFalse()
    }

    @Test
    fun sourceAnchorsComposeWithTheMetadata() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pkg_meta.pks", pkg)), false)
        fun value(name: String) = snapshot.findDeclarations(ProjectQualifiedName.of("pkg_meta", name)).declarations.single() as ProjectPackageValueView
        fun describe(name: String) = snapshot.describeExternalType(value(name).typeRef, ProjectQualifiedName.of("pkg_meta"), full.metadata, schema)
        assertThat(describe("g_name").status).isEqualTo(Status.RESOLVED)
        assertThat(describe("g_name").lookup.get().column.get().datatype.get().length).isEqualTo(OptionalInt.of(30))
        assertThat(names(describe("g_row").lookup.get().relation.get().rowFields)).hasSize(9)
        val copy = describe("g_copy")
        assertThat(copy.status).isEqualTo(Status.RESOLVED)
        assertThat(copy.source.steps.map { it.qualifiedName.toString() }).containsExactly("pkg_meta.g_name")
        assertThat(describe("g_q").status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(describe("g_mixed").status).isEqualTo(Status.RESOLVED)
        // Oracle compiled the very same specification.
        assertThat(owner.call("BEGIN SELECT status INTO ? FROM user_objects WHERE object_name = 'PKG_META' AND object_type = 'PACKAGE'; END;"))
            .isEqualTo("VALID")
    }

    @Test
    fun narrowingFollowsOracleOverloadSelection() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pkg_meta.pks", pkg)), false)
        val structural = snapshot.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("pkg_meta"), ProjectIdentifier.fromSource("take")),
            ProjectCallKind.FUNCTION, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        val number = snapshot.narrowCall(structural, listOf(ProjectArgumentType.builtIn("NUMBER")), full.metadata, schema)
        assertThat(number.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        assertThat(number.selected.get().declaration.parameters.single().typeRef.name.toString()).isEqualTo("test_table.name")
        assertThat(owner.call("BEGIN ? := pkg_meta.take(1); END;")).isEqualTo("name")
        // Without metadata the anchored formals are unknown, so nothing is eliminated.
        assertThat(snapshot.narrowCall(structural, listOf(ProjectArgumentType.builtIn("NUMBER")), ProjectExternalMetadata.none(), schema).status)
            .isEqualTo(ProjectTypedCallResolution.Status.AMBIGUOUS)
        // DATE is compatible with both (DATE converts to VARCHAR2); S9 does not rank, Oracle prefers born.
        assertThat(snapshot.narrowCall(structural, listOf(ProjectArgumentType.builtIn("DATE")), full.metadata, schema).status)
            .isEqualTo(ProjectTypedCallResolution.Status.AMBIGUOUS)
        assertThat(owner.call("BEGIN ? := pkg_meta.take(SYSDATE); END;")).isEqualTo("born")
    }

    @Test
    fun onlyTheOwnSchemaIsDeclaredComplete() {
        assertThat(full.completeSchemas).containsExactly(owner.name)
        assertThat(full.sessionUser).isEqualTo(owner.name)
        assertThat(full.sessionCurrentSchema).isEqualTo(owner.name)
        assertThat(full.metadata.origin.get()).contains(owner.name)
        assertThat(written(Anchor.ROWTYPE, "no_such_table").status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(owner.accepts("DECLARE v no_such_table%ROWTYPE; BEGIN NULL; END;")).isFalse()
        val targeted = OracleMetadataAcquisition.acquire(
            owner.dataSource,
            OracleMetadataRequest.builder().relation(owner.name, "TEST_TABLE").relation(owner.name, "INV_ORDER").relation(owner.name, "GONE").build()
        )
        assertThat(targeted.completeSchemas).isEmpty()
        assertThat(targeted.relationCount).isEqualTo(2)
        assertThat(targeted.notVisible).containsExactly(OracleRelationName.of(owner.name, "GONE"))
        assertThat(relation("TEST_TABLE", targeted).isColumnsComplete).isTrue()
        assertThat(written(Anchor.ROWTYPE, "no_such_table", metadata = targeted.metadata).status).isEqualTo(Status.INCOMPLETE)
        assertThat(written(Anchor.ROWTYPE, "test_table", metadata = targeted.metadata).status).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun aPrivilegeLimitedReaderClaimsNoCompleteness() {
        val result = OracleMetadataAcquisition.acquire(
            reader.dataSource, OracleMetadataRequest.builder().schema(owner.name).relation(owner.name, "DT_TABLE").build()
        )
        assertThat(result.sessionUser).isEqualTo(reader.name)
        assertThat(result.completeSchemas).isEmpty()
        assertThat(result.relationCount).isEqualTo(1)
        assertThat(names(relation("TEST_TABLE", result).rowFields)).hasSize(9)
        assertThat(written(Anchor.ROWTYPE, "dt_table", metadata = result.metadata).status).isEqualTo(Status.INCOMPLETE)
        assertThat(written(Anchor.TYPE, "test_table", "name", metadata = result.metadata).status).isEqualTo(Status.RESOLVED)
    }

    /** A DataSource whose connections record closing and fail on the column query when [failColumns] is set. */
    private class Recording(private val delegate: DataSource, private val failColumns: Boolean) : DataSource by delegate {
        val closed = AtomicBoolean()
        var real: Connection? = null

        override fun getConnection(): Connection {
            val connection = delegate.connection
            real = connection
            return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java)) { _, method, args ->
                if (method.name == "close") closed.set(true)
                if (failColumns && method.name == "prepareStatement" && (args[0] as String).contains("all_tab_cols")) {
                    throw SQLException("simulated failure")
                }
                try {
                    method.invoke(connection, *(args ?: emptyArray()))
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    throw e.targetException
                }
            } as Connection
        }
    }

    @Test
    fun failuresProduceNoResultAndReleaseTheConnection() {
        val failing = Recording(owner.dataSource, failColumns = true)
        assertThatThrownBy { OracleMetadataAcquisition.acquire(failing, OracleMetadataRequest.builder().schema(owner.name).build()) }
            .isInstanceOf(SQLException::class.java).hasMessage("simulated failure")
        assertThat(failing.closed.get()).isTrue()
        assertThat(failing.real!!.isClosed).isTrue()
    }

    @Test
    fun aConnectionInsideAnExternalTransactionIsRefusedAndItsWorkKept() {
        val connection = owner.dataSource.connection
        try {
            connection.autoCommit = false
            connection.createStatement().use { it.executeUpdate("INSERT INTO test_table (id) VALUES (-2)") }
            // A transaction-bound DataSource: hands out the caller's connection and ignores close().
            val bound = object : DataSource by owner.dataSource {
                override fun getConnection(): Connection =
                    Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java)) { _, method, args ->
                        if (method.name == "close") null
                        else try {
                            method.invoke(connection, *(args ?: emptyArray()))
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            throw e.targetException
                        }
                    } as Connection
            }
            assertThatThrownBy { OracleMetadataAcquisition.acquire(bound, OracleMetadataRequest.builder().schema(owner.name).build()) }
                .isInstanceOf(IllegalStateException::class.java)
            assertThat(connection.autoCommit).isFalse()
            connection.createStatement().use { s ->
                s.executeQuery("SELECT COUNT(*) FROM test_table WHERE id = -2").use { rs -> rs.next(); assertThat(rs.getInt(1)).isEqualTo(1) }
            }
        } finally {
            connection.rollback()
            connection.close()
        }
    }

    @Test
    fun theAcquisitionTransactionIsReadOnlyAndTheMetadataDetached() {
        val recording = object : DataSource by owner.dataSource {
            var sawReadOnly = false
            val closed = AtomicBoolean()
            override fun getConnection(): Connection {
                val connection = owner.dataSource.connection
                return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java)) { _, method, args ->
                    if (method.name == "close") closed.set(true)
                    if (method.name == "prepareStatement" && (args[0] as String).contains("SESSION_USER")) {
                        // Inside the acquisition transaction a write must fail with ORA-01456.
                        sawReadOnly = try {
                            connection.createStatement().use { it.executeUpdate("INSERT INTO test_table (id) VALUES (-1)") }
                            false
                        } catch (e: SQLException) {
                            e.errorCode == 1456 || e.message.orEmpty().contains("ORA-01456")
                        }
                    }
                    try {
                        method.invoke(connection, *(args ?: emptyArray()))
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        throw e.targetException
                    }
                } as Connection
            }
        }
        val result = OracleMetadataAcquisition.acquire(recording, OracleMetadataRequest.builder().relation(owner.name, "TEST_TABLE").build())
        assertThat(recording.closed.get()).isTrue()
        assertThat(recording.sawReadOnly).isTrue()
        assertThat(result.metadata.findRelation(schema, id("TEST_TABLE")).status).isEqualTo(ProjectExternalLookup.Status.FOUND)
        assertThat(written(Anchor.TYPE, "test_table", "amount", metadata = result.metadata).status).isEqualTo(Status.RESOLVED)
    }
}
