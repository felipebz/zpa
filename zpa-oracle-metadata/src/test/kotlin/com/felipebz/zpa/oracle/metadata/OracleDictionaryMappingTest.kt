//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics
import com.felipebz.zpa.api.project.ProjectExternalLookup
import com.felipebz.zpa.api.project.ProjectExternalMethodParameter
import com.felipebz.zpa.api.project.ProjectExternalObject.Kind
import com.felipebz.zpa.api.project.ProjectExternalObjectType
import com.felipebz.zpa.api.project.ProjectIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.sql.*
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource

@OptIn(ZpaExperimentalApi::class)
class OracleDictionaryMappingTest {
    private fun n(v: Int?) = v?.let { BigDecimal(it) }

    private fun col(
        name: String, id: Int?, type: String?, owner: String? = null, dataLength: Int? = null, charLength: Int? = null,
        charUsed: String? = null, precision: Int? = null, scale: Int? = null, relation: String = "T", schema: String = "APP",
        nullable: String? = null
    ) = ColumnRow(schema, relation, name, n(id), type, owner, n(dataLength), n(charLength), charUsed, n(precision), n(scale), nullable)

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

    @Test
    fun nullabilityIsKeptWhereTheDictionaryStatesIt() {
        fun nullable(value: String?, trusted: Boolean = true) =
            OracleDictionaryMapping.column(col("CODE", 1, "VARCHAR2", nullable = value), trusted).nullable
        assertThat(nullable("N")).contains(false)
        assertThat(nullable("Y")).contains(true)
        assertThat(nullable(null)).isEmpty()
        // An invalid view's NULLABLE may be out of date: its N is not kept, its Y is.
        assertThat(nullable("N", trusted = false)).isEmpty()
        assertThat(nullable("Y", trusted = false)).contains(true)

        val result = OracleDictionaryMapping.assemble(
            OracleMetadataRequest.builder().schema("APP").build(), session,
            listOf(
                RelationRow("APP", "FLEET", null, null, true),
                RelationRow("APP", "FLEET_V", null, null, true, view = true),
                RelationRow("APP", "STALE_V", null, null, true, view = true, valid = false)
            ),
            listOf("FLEET", "FLEET_V", "STALE_V").map { col("HULL", 1, "VARCHAR2", relation = it, nullable = "N") }, at
        )
        fun hull(relation: String) = result.metadata.findRelation(id("APP"), id(relation)).relation.get().columns.single().nullable
        assertThat(hull("FLEET")).contains(false)
        assertThat(hull("FLEET_V")).contains(false)
        assertThat(hull("STALE_V")).isEmpty()
    }

    private val session = SessionInfo("APP", "APP", "FREEPDB1", "FREE")
    private val at = Instant.parse("2026-01-02T03:04:05Z")

    @Test
    fun objectTypesKeepWhatTheyDeclareAndTheirSupertype() {
        val rows = TypeRows(
            types = listOf(
                TypeRow("APP", "BIN_BASE", "OBJECT", null, null),
                TypeRow("APP", "BIN_ITEM", "OBJECT", "APP", "BIN_BASE"),
                TypeRow("APP", "BIN_LIST", "COLLECTION", null, null),
                TypeRow("SYS", "BLOB_ANY", "ANYDATA", null, null)
            ),
            attributes = listOf(
                TypeAttributeRow("APP", "BIN_BASE", "QTY", null, null, "NUMBER", null, n(6), n(0), null),
                TypeAttributeRow("APP", "BIN_ITEM", "TAG", null, null, "VARCHAR2", n(5), null, null, "C"),
                TypeAttributeRow("APP", "BIN_ITEM", "PEER", "REF", "APP", "BIN_BASE", null, null, null, null)
            ),
            methods = listOf(
                TypeMethodRow("APP", "BIN_ITEM", "LABEL", n(1)!!, n(1)),
                TypeMethodRow("APP", "BIN_ITEM", "MAKE", n(2)!!, n(1)),
                TypeMethodRow("APP", "BIN_ITEM", "RESET", n(3)!!, n(0)),
                TypeMethodRow("SYS", "BLOB_ANY", "WRAP", n(7)!!, n(1))
            ),
            parameters = listOf(
                MethodParameterRow("APP", "BIN_ITEM", "LABEL", n(1)!!, "SELF", "IN", null, "APP", "BIN_ITEM"),
                MethodParameterRow("APP", "BIN_ITEM", "LABEL", n(1)!!, "P_WIDTH", "IN", null, null, "NUMBER"),
                MethodParameterRow("APP", "BIN_ITEM", "MAKE", n(2)!!, "P_QTY", "IN", null, null, "NUMBER"),
                MethodParameterRow("APP", "BIN_ITEM", "RESET", n(3)!!, "SELF", "IN/OUT", null, "APP", "BIN_ITEM"),
                MethodParameterRow("SYS", "BLOB_ANY", "WRAP", n(7)!!, "OBJ", "IN", null, null, "PL/SQL LONG RAW")
            ),
            results = listOf(
                MethodResultRow("APP", "BIN_ITEM", "LABEL", n(1)!!, null, null, "VARCHAR2"),
                MethodResultRow("APP", "BIN_ITEM", "MAKE", n(2)!!, null, "APP", "BIN_ITEM"),
                MethodResultRow("SYS", "BLOB_ANY", "WRAP", n(7)!!, null, "SYS", "BLOB_ANY")
            )
        )
        val result = OracleDictionaryMapping.assemble(OracleMetadataRequest.builder().schema("APP").build(), session, emptyList(), emptyList(), at, types = rows)
        assertThat(listOf(result.objectTypeCount, result.typeAttributeCount, result.typeMethodCount)).containsExactly(4, 3, 4)
        val md = result.metadata
        fun type(owner: String, name: String) = md.findObjectType(id(owner), id(name)).get()
        assertThat(type("APP", "BIN_ITEM").kind).isEqualTo(ProjectExternalObjectType.Kind.OBJECT)
        assertThat(type("APP", "BIN_ITEM").supertypeName.map { it.originalSpelling }).contains("\"BIN_BASE\"")
        assertThat(type("APP", "BIN_BASE").supertypeOwner).isEmpty()
        assertThat(type("APP", "BIN_LIST").kind).isEqualTo(ProjectExternalObjectType.Kind.COLLECTION)
        assertThat(type("SYS", "BLOB_ANY").kind).isEqualTo(ProjectExternalObjectType.Kind.OPAQUE)
        // A type is an object name too.
        assertThat(md.findObject(id("SYS"), id("BLOB_ANY")).map { it.kind }).contains(Kind.TYPE)
        val item = type("APP", "BIN_ITEM")
        assertThat(item.attributes.map { it.name.originalSpelling }).containsExactly("\"TAG\"", "\"PEER\"")
        val tag = item.findAttribute(id("TAG")).get().datatype.get()
        assertThat(listOf(tag.name, tag.length.asInt, tag.lengthSemantics.get())).containsExactly("VARCHAR2", 5, LengthSemantics.CHAR)
        // A REF is not a value of its target type.
        assertThat(item.findAttribute(id("PEER")).get().datatype).isEmpty()
        assertThat(type("APP", "BIN_BASE").findAttribute(id("QTY")).get().datatype.get().precision.asInt).isEqualTo(6)
        val label = item.findMethods(id("LABEL")).single()
        assertThat(listOf(label.isStatic, label.isFunction)).containsExactly(false, true)
        assertThat(label.parameters.map { it.name.originalSpelling }).containsExactly("\"P_WIDTH\"")
        assertThat(label.result.get().name).isEqualTo("VARCHAR2")
        val make = item.findMethods(id("MAKE")).single()
        assertThat(make.isStatic).isTrue()
        assertThat(make.result.get().typeOwner.map { it.originalSpelling }).contains("\"APP\"")
        val reset = item.findMethods(id("RESET")).single()
        assertThat(listOf(reset.isStatic, reset.isFunction, reset.result.isPresent, reset.parameters.isEmpty())).containsExactly(false, false, false, true)
        val wrap = type("SYS", "BLOB_ANY").findMethods(id("WRAP")).single()
        assertThat(wrap.isStatic).isTrue()
        assertThat(wrap.parameters.single().datatype.get().name).isEqualTo("PL/SQL LONG RAW")
        assertThat(wrap.parameters.single().mode).isEqualTo(ProjectExternalMethodParameter.Mode.IN)
        assertThatThrownBy {
            OracleDictionaryMapping.assemble(
                OracleMetadataRequest.builder().schema("APP").build(), session, emptyList(), emptyList(), at,
                types = TypeRows(types = listOf(TypeRow("APP", "BIN_BASE", "OBJECT", null, null), TypeRow("APP", "BIN_BASE", "OBJECT", null, null)))
            )
        }.isInstanceOf(OracleMetadataException::class.java)
    }

    @Test
    fun anAttributeReadWithoutCharUsedHasNoLengthSemantics() {
        val rows = TypeRows(
            types = listOf(TypeRow("APP", "BIN_NOTE", "OBJECT", null, null)),
            attributes = listOf(TypeAttributeRow("APP", "BIN_NOTE", "TEXT", null, null, "VARCHAR2", n(40), null, null, null))
        )
        val result = OracleDictionaryMapping.assemble(OracleMetadataRequest.builder().schema("APP").build(), session, emptyList(), emptyList(), at, types = rows)
        val text = result.metadata.findObjectType(id("APP"), id("BIN_NOTE")).get().findAttribute(id("TEXT")).get().datatype.get()
        assertThat(text.name).isEqualTo("VARCHAR2")
        assertThat(text.lengthSemantics).isEmpty()
    }

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
    fun aSchemaAssertedCompleteIsDeclaredComplete() {
        val request = OracleMetadataRequest.builder().schema("SHARED").schema("OTHER").completeSchema("SHARED").build()
        assertThat(request.completeSchemas).containsExactly("SHARED")
        val result = OracleDictionaryMapping.assemble(
            request, session,
            listOf(RelationRow("SHARED", "T", null, null, true), RelationRow("OTHER", "U", null, null, true)),
            listOf(col("A", 1, "NUMBER", schema = "SHARED"), col("X", 1, "DATE", relation = "U", schema = "OTHER")), at
        )
        assertThat(result.completeSchemas).containsExactly("SHARED")
        val md = result.metadata
        // Absent from a schema asserted complete is absent; elsewhere it is only not covered.
        assertThat(md.findRelation(id("SHARED"), id("NOPE")).status).isEqualTo(ProjectExternalLookup.Status.RELATION_NOT_FOUND)
        assertThat(md.findRelation(id("SHARED"), id("T")).status).isEqualTo(ProjectExternalLookup.Status.FOUND)
        assertThat(md.findRelation(id("OTHER"), id("NOPE")).status).isEqualTo(ProjectExternalLookup.Status.NOT_COVERED)
        // The assertion must name a schema requested for acquisition.
        assertThatThrownBy { OracleMetadataRequest.builder().schema("OTHER").completeSchema("SHARED").build() }
            .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("SHARED")
        assertThatThrownBy { OracleMetadataRequest.builder().relation("SHARED", "T").completeSchema("SHARED").build() }
            .isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().completeSchema("\"x\"") }.isInstanceOf(IllegalArgumentException::class.java)
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
        assertThat(relation.isView).isFalse()
    }

    @Test
    fun viewsAreViewsAndObjectViewsKeepTheirRowType() {
        val result = OracleDictionaryMapping.assemble(
            OracleMetadataRequest.builder().schema("APP").build(), session,
            listOf(RelationRow("APP", "V", null, null, true, view = true), RelationRow("APP", "ADDR_V", "APP", "ADDR_T", true, view = true)),
            listOf(col("A", 1, "NUMBER", relation = "V"), col("STREET", 1, "VARCHAR2", relation = "ADDR_V")), at
        )
        val plain = result.metadata.findRelation(id("APP"), id("V")).relation.get()
        assertThat(plain.isView).isTrue()
        assertThat(plain.rowObjectType).isEmpty()
        val objectView = result.metadata.findRelation(id("APP"), id("ADDR_V")).relation.get()
        assertThat(objectView.isView).isTrue()
        assertThat(objectView.rowObjectType.get().name).isEqualTo("ADDR_T")
        assertThat(objectView.rowObjectType.get().typeOwner).contains(id("APP"))
        assertThat(objectView.rowFields.map { it.name }).containsExactly(id("STREET"))
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

    @Test
    fun objectRowsMapToTheirKinds() {
        val objects = listOf(
            ObjectRow("APP", "SALES_MV", "MATERIALIZED VIEW"), ObjectRow("APP", "SALES_MV", "TABLE"),
            ObjectRow("APP", "PKG_ORDERS", "PACKAGE"), ObjectRow("APP", "PKG_ORDERS", "PACKAGE BODY"),
            ObjectRow("APP", "ADDR_T", "TYPE"), ObjectRow("APP", "ADDR_T", "TYPE BODY"), ObjectRow("APP", "ORDER_SEQ", "SEQUENCE"),
            ObjectRow("APP", "DO_IT", "PROCEDURE"), ObjectRow("APP", "CALC", "FUNCTION"), ObjectRow("APP", "ORDERS_V", "VIEW"),
            ObjectRow("APP", "ORDERS_S", "SYNONYM"), ObjectRow("APP", "ORDERS_IX", "INDEX"), ObjectRow("APP", "ORDERS_TRG", "TRIGGER")
        )
        val result = OracleDictionaryMapping.assemble(OracleMetadataRequest.builder().schema("APP").build(), session, emptyList(), emptyList(), at, objects = objects)
        val md = result.metadata
        fun kind(name: String) = md.findObject(id("APP"), id(name)).map { it.kind }.orElse(null)
        assertThat(kind("SALES_MV")).isEqualTo(Kind.TABLE)
        assertThat(kind("PKG_ORDERS")).isEqualTo(Kind.PACKAGE)
        assertThat(kind("ADDR_T")).isEqualTo(Kind.TYPE)
        assertThat(kind("ORDER_SEQ")).isEqualTo(Kind.SEQUENCE)
        assertThat(kind("DO_IT")).isEqualTo(Kind.PROCEDURE)
        assertThat(kind("CALC")).isEqualTo(Kind.FUNCTION)
        assertThat(kind("ORDERS_V")).isEqualTo(Kind.VIEW)
        assertThat(kind("ORDERS_S")).isEqualTo(Kind.SYNONYM)
        assertThat(kind("ORDERS_IX")).isNull()
        assertThat(kind("ORDERS_TRG")).isNull()
        assertThat(result.objectCount).isEqualTo(8)
        assertThat(OracleDictionaryMapping.objectKind("JAVA CLASS")).isNull()
        // Two kinds under one name cannot come from a consistent dictionary.
        assertThatThrownBy {
            OracleDictionaryMapping.assemble(
                OracleMetadataRequest.builder().schema("APP").build(), session, emptyList(), emptyList(), at,
                objects = listOf(ObjectRow("APP", "CALC", "FUNCTION"), ObjectRow("APP", "CALC", "PROCEDURE"))
            )
        }.isInstanceOf(OracleMetadataException::class.java).hasMessageContaining("APP.CALC")
    }

    @Test
    fun synonymRowsMapIncludingPublicOwnersPublicTargetsAndDatabaseLinks() {
        val synonyms = listOf(
            SynonymRow("APP", "CUST", "OTHER", "CUSTOMERS", null),
            SynonymRow("APP", "FAR_ORDERS", null, "ORDERS", "FAR_LINK.EXAMPLE.COM"),
            SynonymRow("APP", "VIA_PUBLIC", "PUBLIC", "SHARED_CODES", null),
            SynonymRow("PUBLIC", "SHARED_CODES", "OTHER", "CODES", null),
            SynonymRow("PUBLIC", "DANGLING", "OTHER", "NO_SUCH_TABLE", null)
        )
        val request = OracleMetadataRequest.builder().schema("APP").publicSynonyms().build()
        val result = OracleDictionaryMapping.assemble(request, session, emptyList(), emptyList(), at, synonyms = synonyms)
        val md = result.metadata
        assertThat(result.synonymCount).isEqualTo(5)
        val cust = md.findSynonym(id("APP"), id("CUST")).get()
        assertThat(cust.isPublic).isFalse()
        assertThat(cust.targetOwner).contains(id("OTHER"))
        assertThat(cust.targetName).isEqualTo(id("CUSTOMERS"))
        assertThat(cust.dbLink).isEmpty()
        val far = md.findSynonym(id("APP"), id("FAR_ORDERS")).get()
        assertThat(far.targetOwner).isEmpty()
        assertThat(far.dbLink).contains("FAR_LINK.EXAMPLE.COM")
        assertThat(md.findSynonym(id("APP"), id("VIA_PUBLIC")).get().targetOwner).contains(id("PUBLIC"))
        val shared = md.findSynonym(id("PUBLIC"), id("SHARED_CODES")).get()
        assertThat(shared.isPublic).isTrue()
        assertThat(md.findObject(id("PUBLIC"), id("DANGLING")).get().kind).isEqualTo(Kind.SYNONYM)
        assertThat(md.findObject(id("APP"), id("FAR_ORDERS")).get().kind).isEqualTo(Kind.SYNONYM)
        // Rows that cannot form a synonym fail the acquisition.
        for (bad in listOf(
            listOf(SynonymRow("APP", "CUST", "OTHER", "CUSTOMERS", null), SynonymRow("APP", "CUST", "OTHER", "CLIENTS", null)),
            listOf(SynonymRow("APP", "LOCAL", null, "ORDERS", null)),
            listOf(SynonymRow("APP", "NO_TARGET", "OTHER", null, null))
        )) {
            assertThatThrownBy { OracleDictionaryMapping.assemble(request, session, emptyList(), emptyList(), at, synonyms = bad) }
                .isInstanceOf(OracleMetadataException::class.java).hasMessageContaining("synonym APP.")
        }
    }

    @Test
    fun objectsOfACompleteSchemaProveAbsenceAndOthersDoNot() {
        val request = OracleMetadataRequest.builder().schema("APP").schema("SHARED").schema("OTHER").completeSchema("SHARED").build()
        val result = OracleDictionaryMapping.assemble(
            request, session, listOf(RelationRow("APP", "T", null, null, true)), emptyList(), at,
            objects = listOf(ObjectRow("APP", "PKG_ORDERS", "PACKAGE"), ObjectRow("SHARED", "CALC", "FUNCTION"), ObjectRow("OTHER", "DO_IT", "PROCEDURE")),
            synonyms = listOf(SynonymRow("OTHER", "CUST", "APP", "T", null))
        )
        val md = result.metadata
        for (complete in listOf("APP", "SHARED")) {
            assertThat(md.isSchemaComplete(id(complete))).isTrue()
            assertThat(md.findObject(id(complete), id("NOPE"))).isEmpty()
        }
        assertThat(md.findObject(id("APP"), id("PKG_ORDERS")).get().kind).isEqualTo(Kind.PACKAGE)
        assertThat(md.findObject(id("APP"), id("T")).get().kind).isEqualTo(Kind.TABLE)
        assertThat(md.findObject(id("SHARED"), id("CALC")).get().kind).isEqualTo(Kind.FUNCTION)
        // Seen through privileges only: what is found is found, but absence proves nothing.
        assertThat(md.isSchemaComplete(id("OTHER"))).isFalse()
        assertThat(md.findObject(id("OTHER"), id("DO_IT")).get().kind).isEqualTo(Kind.PROCEDURE)
        assertThat(md.findObject(id("OTHER"), id("CUST")).get().kind).isEqualTo(Kind.SYNONYM)
        assertThat(md.findObject(id("OTHER"), id("NOPE"))).isEmpty()
    }

    @Test
    fun publicSynonymsAreCompleteOnlyWhenAllWereRequested() {
        val row = SynonymRow("PUBLIC", "SHARED_CODES", "OTHER", "CODES", null)
        fun assembled(request: OracleMetadataRequest, rows: List<SynonymRow>) =
            OracleDictionaryMapping.assemble(request, session, emptyList(), emptyList(), at, synonyms = rows).metadata
        assertThat(assembled(OracleMetadataRequest.builder().publicSynonyms().build(), listOf(row)).isPublicSynonymsComplete).isTrue()
        // An empty unrestricted answer is a complete answer: there is no public synonym.
        assertThat(assembled(OracleMetadataRequest.builder().publicSynonyms().build(), emptyList()).isPublicSynonymsComplete).isTrue()
        val restricted = assembled(OracleMetadataRequest.builder().publicSynonymsFor("OTHER").build(), listOf(row))
        assertThat(restricted.isPublicSynonymsComplete).isFalse()
        assertThat(restricted.findSynonym(id("PUBLIC"), id("SHARED_CODES"))).isPresent()
        assertThat(assembled(OracleMetadataRequest.builder().schema("APP").build(), emptyList()).isPublicSynonymsComplete).isFalse()
        // The unrestricted request supersedes any restriction.
        val both = OracleMetadataRequest.builder().publicSynonymsFor("OTHER").publicSynonyms().build()
        assertThat(both.publicSynonymTargets).isEmpty()
        assertThat(assembled(both, listOf(row)).isPublicSynonymsComplete).isTrue()
    }

    @Test
    fun publicSynonymRequestsAreValidated() {
        assertThatThrownBy { OracleMetadataRequest.builder().publicSynonymsFor(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().publicSynonymsFor("\"OTHER\"") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OracleMetadataRequest.builder().schema("PUBLIC") }
            .isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("publicSynonyms()")
        assertThatThrownBy { OracleMetadataRequest.builder().build() }
            .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("public synonym")
        val all = OracleMetadataRequest.builder().publicSynonyms().build()
        assertThat(all.publicSynonyms).isTrue()
        assertThat(all.schemas).isEmpty()
        assertThat(all.relations).isEmpty()
        val restricted = OracleMetadataRequest.builder().publicSynonymsFor("OTHER").publicSynonymsFor("SHARED").publicSynonymsFor("OTHER").build()
        assertThat(restricted.publicSynonyms).isTrue()
        assertThat(restricted.publicSynonymTargets).containsExactly("OTHER", "SHARED")
        assertThat(restricted.toString()).contains("publicSynonyms=true", "publicSynonymTargets=[OTHER, SHARED]")
        assertThatThrownBy { (restricted.publicSynonymTargets as MutableSet<String>).add("X") }.isInstanceOf(UnsupportedOperationException::class.java)
        val none = OracleMetadataRequest.builder().schema("APP").build()
        assertThat(none.publicSynonyms).isFalse()
        assertThat(none.publicSynonymTargets).isEmpty()
    }

    @Test
    fun objectsAndSynonymsAreReadInTheTransactionWithBoundNamesAndTheTimeout() {
        val scripted = Scripted { sql ->
            when {
                sql.contains(OBJECTS) -> listOf(listOf("APP", "PKG_ORDERS", "PACKAGE"), listOf("APP", "SALES_MV", "MATERIALIZED VIEW"))
                sql.contains("synonym_name") && sql.contains("owner = ?") -> listOf(listOf("APP", "FAR_ORDERS", null, "ORDERS", "FAR_LINK.EXAMPLE.COM"))
                sql.contains("synonym_name") -> listOf(listOf("PUBLIC", "SHARED_CODES", "OTHER", "CODES", null))
                else -> emptyList()
            }
        }
        val request = OracleMetadataRequest.builder().schema("APP").publicSynonymsFor("OTHER").publicSynonymsFor("SHARED")
            .queryTimeout(Duration.ofSeconds(30)).build()
        val result = OracleMetadataAcquisition.acquire(scripted.dataSource, request)
        assertThat(scripted.parameters(OBJECTS)).containsExactly(listOf("APP"))
        assertThat(scripted.parameters("db_link FROM all_synonyms WHERE owner = ?")).containsExactly(listOf("APP"))
        assertThat(scripted.parameters("owner = 'PUBLIC' AND table_owner IN (?, ?)")).containsExactly(listOf("OTHER", "SHARED"))
        assertThat(scripted.statements.values).allSatisfy { verify(it).queryTimeout = 30 }
        inOrder(scripted.connection) {
            verify(scripted.connection).autoCommit = false
            verify(scripted.connection).prepareStatement(argThat { contains(OBJECTS) })
            verify(scripted.connection).prepareStatement(argThat { contains("owner = 'PUBLIC'") })
            verify(scripted.connection).rollback()
        }
        assertThat(result.objectCount).isEqualTo(2)
        assertThat(result.synonymCount).isEqualTo(2)
        assertThat(result.toString()).contains("2 objects", "2 synonyms", "publicSynonymsComplete=false")
        val md = result.metadata
        assertThat(md.findObject(id("APP"), id("SALES_MV")).get().kind).isEqualTo(Kind.TABLE)
        assertThat(md.findSynonym(id("APP"), id("FAR_ORDERS")).get().dbLink).contains("FAR_LINK.EXAMPLE.COM")
        assertThat(md.findSynonym(id("PUBLIC"), id("SHARED_CODES"))).isPresent()
        assertThat(md.isPublicSynonymsComplete).isFalse()
    }

    @Test
    fun publicSynonymsAreQueriedOnlyWhenRequested() {
        val withoutPublic = Scripted { emptyList() }
        val schemaOnly = OracleMetadataAcquisition.acquire(withoutPublic.dataSource, OracleMetadataRequest.builder().schema("APP").build())
        assertThat(withoutPublic.statements.keys).noneMatch { it.contains("PUBLIC") }
        assertThat(schemaOnly.metadata.isPublicSynonymsComplete).isFalse()

        val all = Scripted { sql -> if (sql.contains("synonym_name")) listOf(listOf("PUBLIC", "SHARED_CODES", "OTHER", "CODES", null)) else emptyList() }
        val publicOnly = OracleMetadataAcquisition.acquire(all.dataSource, OracleMetadataRequest.builder().publicSynonyms().build())
        assertThat(all.statements.keys.filter { it.contains("synonym_name") })
            .containsExactly("SELECT owner, synonym_name, table_owner, table_name, db_link FROM all_synonyms WHERE owner = 'PUBLIC'")
        assertThat(all.parameters("synonym_name")).containsExactly(emptyList())
        assertThat(publicOnly.metadata.isPublicSynonymsComplete).isTrue()
        assertThat(publicOnly.metadata.findSynonym(id("PUBLIC"), id("SHARED_CODES"))).isPresent()
    }

    @Test
    fun objectTypesAreReadWithTheirSupertypesEachOnce() {
        fun scope(sql: String) = sql.substringAfter("WHERE ")
        val scripted = Scripted { sql ->
            when {
                // The schema's own types; the types its synonyms stand for repeat one of them.
                sql.contains("FROM all_types") && scope(sql) == "owner = ?" -> listOf(listOf("APP", "BIN_ITEM", "OBJECT", "BASE", "BIN_SHAPE"))
                sql.contains("FROM all_types") && sql.contains("all_synonyms") -> listOf(listOf("APP", "BIN_ITEM", "OBJECT", "BASE", "BIN_SHAPE"))
                sql.contains("FROM all_types") && sql.contains("(owner, type_name) IN ((?, ?))") -> listOf(listOf("BASE", "BIN_SHAPE", "OBJECT", null, null))
                sql.contains("FROM all_type_methods") && scope(sql).contains("owner = ?") -> listOf(listOf("APP", "BIN_ITEM", "LABEL", "1", "1"))
                else -> emptyList()
            }
        }
        val result = OracleMetadataAcquisition.acquire(scripted.dataSource, OracleMetadataRequest.builder().schema("APP").build())
        assertThat(scripted.parameters("FROM all_types WHERE (owner, type_name) IN ((?, ?))")).containsExactly(listOf("BASE", "BIN_SHAPE"))
        // No type was new to the synonyms' query: what the types declare is not read again for it.
        assertThat(scripted.statements.keys.filter { it.contains("all_type_attrs") && it.contains("all_synonyms") }).isEmpty()
        assertThat(listOf(result.objectTypeCount, result.typeMethodCount)).containsExactly(2, 1)
        assertThat(result.metadata.findObjectType(id("BASE"), id("BIN_SHAPE"))).isPresent()
        assertThat(result.metadata.findObjectType(id("APP"), id("BIN_ITEM")).get().findMethods(id("LABEL")).single().isFunction).isTrue()
        assertThat(result.toString()).contains("2 object types (0 attributes, 1 methods")
    }

    @Test
    fun theAcquisitionReadsNullabilityAndTheStatusOfViews() {
        fun columnRow(relation: String, nullable: String) =
            listOf("APP", relation, "HULL", null, "VARCHAR2", null, null, null, null, null, null, nullable)
        val scripted = Scripted { sql ->
            when {
                sql.contains("all_all_tables") -> listOf(
                    listOf("APP", "FLEET", null, null, "TABLE", null),
                    listOf("APP", "FLEET_V", null, null, "VIEW", "VALID"),
                    listOf("APP", "STALE_V", null, null, "VIEW", "INVALID")
                )
                sql.contains("all_tab_cols") -> listOf(columnRow("FLEET", "N"), columnRow("FLEET_V", "N"), columnRow("STALE_V", "N"))
                else -> emptyList()
            }
        }
        val metadata = OracleMetadataAcquisition.acquire(scripted.dataSource, OracleMetadataRequest.builder().schema("APP").build()).metadata
        fun hull(relation: String) = metadata.findRelation(id("APP"), id(relation)).relation.get().columns.single().nullable
        assertThat(hull("FLEET")).contains(false)
        assertThat(hull("FLEET_V")).contains(false)
        assertThat(hull("STALE_V")).isEmpty()
    }

    private companion object {
        /** The object query; the relation query reads `ALL_OBJECTS` too, for the status of views. */
        const val OBJECTS = "FROM all_objects WHERE owner = ?"
    }

    /** A connection answering each prepared statement with the rows [rows] gives for its SQL; records the statements. */
    private inner class Scripted(private val rows: (String) -> List<List<String?>>) {
        val statements = LinkedHashMap<String, PreparedStatement>()
        private val serverMetaData = databaseMetaData(23)
        private val transactionStatement = mock<Statement>()
        val connection: Connection = mock {
            on { autoCommit } doReturn true
            on { metaData } doReturn serverMetaData
            on { createStatement() } doReturn transactionStatement
            on { prepareCall(any()) } doThrow SQLException("DBMS_DB_VERSION is not available")
            on { prepareStatement(any()) } doAnswer { invocation -> statement(invocation.getArgument(0)) }
        }
        val dataSource: DataSource = mock { on { this.connection } doReturn connection }

        /** The bound parameters of every statement whose SQL contains [fragment], in execution order. */
        fun parameters(fragment: String): List<List<String>> = statements.filterKeys { it.contains(fragment) }.values.map { statement ->
            mockingDetails(statement).invocations.filter { it.method.name == "setString" }.map { it.getArgument<String>(1) }
        }

        private fun statement(sql: String): PreparedStatement {
            val answer = if (sql.contains("SESSION_USER")) listOf(listOf("APP", "APP", "FREEPDB1", "FREE")) else rows(sql)
            val resultSet = resultSet(answer)
            val statement = mock<PreparedStatement> { on { executeQuery() } doReturn resultSet }
            check(statements.put(sql, statement) == null) { "Statement prepared twice: $sql" }
            return statement
        }

        private fun resultSet(answer: List<List<String?>>): ResultSet {
            var index = -1
            return mock {
                on { next() } doAnswer { ++index < answer.size }
                on { getString(any<Int>()) } doAnswer { answer[index][it.getArgument<Int>(0) - 1] }
                on { getBigDecimal(any<Int>()) } doAnswer { answer[index][it.getArgument<Int>(0) - 1]?.let(::BigDecimal) }
            }
        }
    }

    private fun databaseMetaData(major: Int) = mock<java.sql.DatabaseMetaData> { on { databaseMajorVersion } doReturn major }
}
