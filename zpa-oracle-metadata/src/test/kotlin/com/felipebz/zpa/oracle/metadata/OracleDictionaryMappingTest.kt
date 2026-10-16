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
import com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics
import com.felipebz.zpa.api.project.ProjectExternalLookup
import com.felipebz.zpa.api.project.ProjectIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource

@OptIn(ZpaExperimentalApi::class)
class OracleDictionaryMappingTest {
    private fun n(v: Int?) = v?.let { BigDecimal(it) }

    private fun col(
        name: String, id: Int?, type: String?, owner: String? = null, dataLength: Int? = null, charLength: Int? = null,
        charUsed: String? = null, precision: Int? = null, scale: Int? = null, relation: String = "T", schema: String = "APP"
    ) = ColumnRow(schema, relation, name, n(id), type, owner, n(dataLength), n(charLength), charUsed, n(precision), n(scale))

    private fun id(s: String) = ProjectIdentifier.fromSource("\"$s\"")

    @Test
    fun datatypesKeepDeclaredFactsOnly() {
        val number = OracleDictionaryMapping.datatype(col("N", 1, "NUMBER", dataLength = 22))!!
        assertThat(number.length).isEqualTo(OptionalInt.empty())
        assertThat(number.precision).isEqualTo(OptionalInt.empty())
        assertThat(number.scale).isEqualTo(OptionalInt.empty())
        val money = OracleDictionaryMapping.datatype(col("M", 1, "NUMBER", dataLength = 22, precision = 10, scale = 2))!!
        assertThat(money.precision).isEqualTo(OptionalInt.of(10))
        assertThat(money.scale).isEqualTo(OptionalInt.of(2))
        val negative = OracleDictionaryMapping.datatype(col("X", 1, "NUMBER", precision = 5, scale = -2))!!
        assertThat(negative.scale).isEqualTo(OptionalInt.of(-2))
        val zeroScale = OracleDictionaryMapping.datatype(col("I", 1, "NUMBER", scale = 0))!!
        assertThat(zeroScale.scale).isEqualTo(OptionalInt.of(0))
        assertThat(zeroScale.precision).isEqualTo(OptionalInt.empty())
        val bytes = OracleDictionaryMapping.datatype(col("V", 1, "VARCHAR2", dataLength = 30, charLength = 30, charUsed = "B"))!!
        assertThat(bytes.length).isEqualTo(OptionalInt.of(30))
        assertThat(bytes.lengthSemantics).contains(LengthSemantics.BYTE)
        val chars = OracleDictionaryMapping.datatype(col("V", 1, "VARCHAR2", dataLength = 80, charLength = 20, charUsed = "C"))!!
        assertThat(chars.length).isEqualTo(OptionalInt.of(20))
        assertThat(chars.lengthSemantics).contains(LengthSemantics.CHAR)
        val raw = OracleDictionaryMapping.datatype(col("R", 1, "RAW", dataLength = 16))!!
        assertThat(raw.length).isEqualTo(OptionalInt.of(16))
        assertThat(raw.lengthSemantics).contains(LengthSemantics.BYTE)
        listOf("DATE" to 7, "CLOB" to 4000, "BLOB" to 4000, "TIMESTAMP(6)" to 11).forEach { (type, storage) ->
            assertThat(OracleDictionaryMapping.datatype(col("D", 1, type, dataLength = storage))!!.length).describedAs(type).isEqualTo(OptionalInt.empty())
        }
        val xml = OracleDictionaryMapping.datatype(col("X", 1, "XMLTYPE", owner = "SYS", dataLength = 2000))!!
        assertThat(xml.typeOwner).contains(id("SYS"))
        assertThat(xml.length).isEqualTo(OptionalInt.empty())
        val ts = OracleDictionaryMapping.datatype(col("T", 1, "TIMESTAMP(6) WITH TIME ZONE", scale = 6))!!
        assertThat(ts.name).isEqualTo("TIMESTAMP(6) WITH TIME ZONE")
        assertThat(OracleDictionaryMapping.datatype(col("U", 1, null))).isNull()
    }

    @Test
    fun invisibleColumnsKeepNoPosition() {
        assertThat(OracleDictionaryMapping.column(col("SECRET", null, "VARCHAR2", dataLength = 10, charLength = 10, charUsed = "B")).position)
            .isEqualTo(OptionalInt.empty())
        assertThat(OracleDictionaryMapping.column(col("ID", 3, "NUMBER")).position).isEqualTo(OptionalInt.of(3))
    }

    private val session = SessionInfo("APP", "APP", "FREEPDB1", "FREE")
    private val at = Instant.parse("2026-01-02T03:04:05Z")

    @Test
    fun ownSchemaIsCompleteAndOtherSchemasAreNot() {
        val request = OracleMetadataRequest.builder().schema("APP").schema("OTHER").relation("THIRD", "T").relation("THIRD", "GONE").build()
        val result = OracleDictionaryMapping.assemble(
            request, session,
            listOf(RelationRow("APP", "T", null, null, true), RelationRow("OTHER", "U", null, null, true), RelationRow("THIRD", "T", null, null, true)),
            listOf(
                col("B", 2, "NUMBER"), col("A", 1, "NUMBER"), col("SECRET", null, "NUMBER"),
                col("X", 1, "DATE", relation = "U", schema = "OTHER"), col("Y", 1, "DATE", relation = "T", schema = "THIRD"),
                col("ORPHAN", 1, "DATE", relation = "BIN\$1", schema = "APP")
            ),
            at
        )
        assertThat(result.completeSchemas).containsExactly("APP")
        assertThat(result.notVisible).containsExactly(OracleRelationName.of("THIRD", "GONE"))
        assertThat(result.relationCount).isEqualTo(3)
        assertThat(result.columnCount).isEqualTo(5)
        assertThat(result.metadata.origin).contains("oracle FREE/FREEPDB1 as APP at 2026-01-02T03:04:05Z")
        val md = result.metadata
        val t = md.findRelation(id("APP"), id("T")).relation.get()
        assertThat(t.isColumnsComplete).isTrue()
        assertThat(t.rowFields.map { it.name.originalSpelling }).containsExactly("\"A\"", "\"B\"")
        assertThat(t.columns.map { it.name.originalSpelling }).containsExactly("\"A\"", "\"B\"", "\"SECRET\"")
        assertThat(md.findRelation(id("APP"), id("NOPE")).status).isEqualTo(ProjectExternalLookup.Status.RELATION_NOT_FOUND)
        assertThat(md.findRelation(id("OTHER"), id("NOPE")).status).isEqualTo(ProjectExternalLookup.Status.NOT_COVERED)
        assertThat(md.findRelation(id("THIRD"), id("GONE")).status).isEqualTo(ProjectExternalLookup.Status.NOT_COVERED)
        assertThat(md.findColumn(id("APP"), id("T"), id("NOPE")).status).isEqualTo(ProjectExternalLookup.Status.COLUMN_NOT_FOUND)
        assertThatThrownBy { (result.completeSchemas as MutableSet<String>).add("X") }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (result.notVisible as MutableSet<OracleRelationName>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun columnCompletenessFollowsTheAcquisitionNotTheRowCount() {
        val request = OracleMetadataRequest.builder().relation("APP", "V").relation("APP", "W").build()
        val result = OracleDictionaryMapping.assemble(
            request, session, listOf(RelationRow("APP", "V", null, null, true), RelationRow("APP", "W", null, null, false)), emptyList(), at
        )
        assertThat(result.completeSchemas).isEmpty()
        assertThat(result.metadata.findRelation(id("APP"), id("V")).relation.get().isColumnsComplete).isTrue()
        assertThat(result.metadata.findColumn(id("APP"), id("V"), id("X")).status).isEqualTo(ProjectExternalLookup.Status.COLUMN_NOT_FOUND)
        assertThat(result.metadata.findRelation(id("APP"), id("W")).relation.get().isColumnsComplete).isFalse()
        assertThat(result.metadata.findColumn(id("APP"), id("W"), id("X")).status).isEqualTo(ProjectExternalLookup.Status.NOT_COVERED)
    }

    @Test
    fun duplicateRelationRowsFailWithoutAResult() {
        assertThatThrownBy {
            OracleDictionaryMapping.assemble(
                OracleMetadataRequest.builder().schema("APP").build(), session,
                listOf(RelationRow("APP", "T", null, null, true), RelationRow("APP", "T", null, null, true)), listOf(col("A", 1, "NUMBER")), at
            )
        }.isInstanceOf(OracleMetadataException::class.java).hasMessageContaining("APP.T more than once")
    }

    @Test
    fun objectTablesKeepTheirRowType() {
        val result = OracleDictionaryMapping.assemble(
            OracleMetadataRequest.builder().schema("APP").build(), session,
            listOf(RelationRow("APP", "ADDR_TAB", "APP", "ADDR_T", true)),
            listOf(col("STREET", 1, "VARCHAR2", relation = "ADDR_TAB"), col("CITY", 2, "VARCHAR2", relation = "ADDR_TAB")), at
        )
        val relation = result.metadata.findRelation(id("APP"), id("ADDR_TAB")).relation.get()
        assertThat(relation.rowObjectType.get().name).isEqualTo("ADDR_T")
        assertThat(relation.rowObjectType.get().typeOwner).contains(id("APP"))
        assertThat(relation.rowFields).hasSize(2)
    }

    @Test
    fun inconsistentRowsFailWithoutAResult() {
        val request = OracleMetadataRequest.builder().schema("APP").build()
        assertThatThrownBy {
            OracleDictionaryMapping.assemble(request, session, listOf(RelationRow("APP", "T", null, null, true)), listOf(col("A", 1, "NUMBER"), col("B", 1, "NUMBER")), at)
        }.isInstanceOf(OracleMetadataException::class.java).hasMessageContaining("APP.T")
        assertThatThrownBy {
            OracleDictionaryMapping.assemble(request, session, listOf(RelationRow("APP", "T", null, null, true)), listOf(col("A", 1, "NUMBER"), col("A", 2, "NUMBER")), at)
        }.isInstanceOf(OracleMetadataException::class.java)
    }

    @Test
    fun requestsAreValidatedAndImmutable() {
        assertThatThrownBy { OracleMetadataRequest.builder().build() }.isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().schema(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().relation("A", "\"B\"") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().queryTimeout(Duration.ofMillis(10)) }.isInstanceOf(IllegalArgumentException::class.java)
        val builder = OracleMetadataRequest.builder().schema("A").relation("B", "T").relation("B", "T")
        val request = builder.build()
        builder.schema("C")
        assertThat(request.schemas).containsExactly("A")
        assertThat(request.relations).containsExactly(OracleRelationName.of("B", "T"))
        assertThat(request.queryTimeout).isEqualTo(Duration.ofSeconds(60))
        assertThatThrownBy { (request.schemas as MutableSet<String>).add("X") }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun failuresRollBackRestoreAndCloseWithoutAResult() {
        val statement = mock<Statement>()
        val serverMetaData = databaseMetaData(23)
        val connection = mock<Connection> {
            on { autoCommit } doReturn true
            on { metaData } doReturn serverMetaData
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doThrow SQLException("ORA-00942: table or view does not exist")
        }
        val dataSource = mock<DataSource> { on { this.connection } doReturn connection }
        assertThatThrownBy { OracleMetadataAcquisition.acquire(dataSource, OracleMetadataRequest.builder().schema("APP").build()) }
            .isInstanceOf(SQLException::class.java).hasMessageContaining("ORA-00942")
        inOrder(connection, statement) {
            verify(connection).autoCommit = false
            verify(statement).execute("SET TRANSACTION READ ONLY")
            verify(connection).rollback()
            verify(connection).autoCommit = true
            verify(connection).close()
        }
        verify(connection, times(1)).rollback()
    }

    @Test
    fun aConnectionOutsideAutoCommitIsRefusedUntouched() {
        val connection = mock<Connection> { on { autoCommit } doReturn false }
        val dataSource = mock<DataSource> { on { this.connection } doReturn connection }
        assertThatThrownBy { OracleMetadataAcquisition.acquire(dataSource, OracleMetadataRequest.builder().schema("APP").build()) }
            .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("auto-commit")
        verify(connection, never()).rollback()
        verify(connection, never()).autoCommit = any()
        verify(connection, never()).createStatement()
        verify(connection, never()).prepareStatement(any())
        verify(connection).close()
    }

    @Test
    fun autoCommitIsRestoredEvenWhenTheFinalRollbackFails() {
        val statement = mock<Statement>()
        val serverMetaData = databaseMetaData(23)
        val connection = mock<Connection> {
            on { autoCommit } doReturn true
            on { metaData } doReturn serverMetaData
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doThrow SQLException("primary")
            on { rollback() } doThrow SQLException("rollback failed")
        }
        val dataSource = mock<DataSource> { on { this.connection } doReturn connection }
        assertThatThrownBy { OracleMetadataAcquisition.acquire(dataSource, OracleMetadataRequest.builder().schema("APP").build()) }
            .isInstanceOf(SQLException::class.java).hasMessage("primary")
            .satisfies({ e -> assertThat(e.suppressed.map { it.message }).contains("rollback failed") })
        verify(connection).autoCommit = true
        verify(connection).close()
    }

    private fun databaseMetaData(major: Int) = mock<java.sql.DatabaseMetaData> { on { databaseMajorVersion } doReturn major }
}
