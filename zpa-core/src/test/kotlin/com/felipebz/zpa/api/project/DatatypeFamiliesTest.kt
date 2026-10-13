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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.DatatypeCompatibility.*
import com.felipebz.zpa.api.project.ProjectDatatypeFamily.*
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Datatype families and the actual-to-formal compatibility relation. Every expected cell restates a measurement on
 * Oracle 26ai: a single procedure with one formal of the formal type, called with a literal or variable of the actual
 * type, compiles (COMPATIBLE) or fails with PLS-00306 (INCOMPATIBLE) for every member pair and for IN, OUT and IN OUT;
 * a runtime conversion error still counts as compiling.
 */
@OptIn(ZpaExperimentalApi::class)
class DatatypeFamiliesTest {
    private fun name(vararg s: String) = QualifiedName(s.map { OracleIdentifier.fromSource(it) })

    /** Every supported built-in spelling per family, as the source type reference retains it. */
    private val members = mapOf(
        NUMERIC to listOf(
            "NUMBER", "INTEGER", "INT", "SMALLINT", "DEC", "DECIMAL", "NUMERIC", "FLOAT", "REAL", "DOUBLE", "PLS_INTEGER",
            "BINARY_INTEGER", "NATURAL", "NATURALN", "POSITIVE", "POSITIVEN", "SIGNTYPE", "SIMPLE_INTEGER", "BINARY_FLOAT", "BINARY_DOUBLE"
        ),
        CHARACTER to listOf("VARCHAR2", "VARCHAR", "CHAR", "CHARACTER", "STRING", "NCHAR", "NVARCHAR2"),
        CLOB to listOf("CLOB", "NCLOB"),
        BLOB to listOf("BLOB"),
        RAW to listOf("RAW"),
        DATETIME to listOf("DATE", "TIMESTAMP"),
        INTERVAL to listOf("INTERVAL"),
        BOOLEAN to listOf("BOOLEAN"),
        JSON to listOf("JSON"),
        ROWID to listOf("ROWID", "UROWID")
    )

    /** Measured INCOMPATIBLE and UNKNOWN formal families per actual family; every other formal family is COMPATIBLE. */
    private val incompatible = mapOf(
        NUMERIC to setOf(CLOB, BLOB, RAW, DATETIME, INTERVAL, JSON, ROWID, COMPOSITE),
        CHARACTER to setOf(BLOB, JSON, COMPOSITE),
        CLOB to setOf(NUMERIC, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID, COMPOSITE),
        BLOB to setOf(NUMERIC, CHARACTER, CLOB, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID, COMPOSITE),
        RAW to setOf(NUMERIC, CLOB, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID, COMPOSITE),
        DATETIME to setOf(NUMERIC, CLOB, BLOB, RAW, INTERVAL, BOOLEAN, JSON, ROWID, COMPOSITE),
        INTERVAL to setOf(NUMERIC, CLOB, BLOB, RAW, DATETIME, BOOLEAN, JSON, ROWID, COMPOSITE),
        BOOLEAN to setOf(CLOB, BLOB, RAW, DATETIME, INTERVAL, JSON, ROWID, COMPOSITE),
        JSON to setOf(NUMERIC, CHARACTER, CLOB, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, ROWID, COMPOSITE),
        ROWID to setOf(NUMERIC, CLOB, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, JSON, COMPOSITE),
        COMPOSITE to setOf(NUMERIC, CHARACTER, CLOB, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID)
    )
    /** Depends on PLSQL_IMPLICIT_CONVERSION_BOOL (measured both ways). */
    private val environment = mapOf(NUMERIC to setOf(BOOLEAN), CHARACTER to setOf(BOOLEAN), BOOLEAN to setOf(NUMERIC, CHARACTER))
    private val unknown = mapOf(INTERVAL to setOf(INTERVAL), COMPOSITE to setOf(COMPOSITE))

    private fun expected(actual: ProjectDatatypeFamily, formal: ProjectDatatypeFamily) = when (formal) {
        in incompatible.getValue(actual) -> INCOMPATIBLE
        in environment[actual].orEmpty() -> ENVIRONMENT_DEPENDENT
        in unknown[actual].orEmpty() -> UNKNOWN
        else -> COMPATIBLE
    }

    @Test
    fun everyCellOfTheRelationMatchesTheMeasurement() {
        for (actual in ProjectDatatypeFamily.entries) for (formal in ProjectDatatypeFamily.entries) {
            assertThat(DatatypeFamilies.compatibility(actual, formal)).describedAs("$actual -> $formal").isEqualTo(expected(actual, formal))
        }
    }

    @Test
    fun everyAliasPairFollowsItsFamilies() {
        for ((actualFamily, actualNames) in members) for ((formalFamily, formalNames) in members) {
            for (a in actualNames) for (f in formalNames) {
                val result = DatatypeFamilies.compatibility(DatatypeFamilies.ofName(name(a)), DatatypeFamilies.ofName(name(f)))
                assertThat(result).describedAs("$a -> $f").isEqualTo(expected(actualFamily, formalFamily))
            }
        }
    }

    @Test
    fun theRelationIsNotSymmetric() {
        fun c(a: String, f: String) = DatatypeFamilies.compatibility(DatatypeFamilies.ofName(name(a)), DatatypeFamilies.ofName(name(f)))
        assertThat(c("NUMBER", "VARCHAR2")).isEqualTo(COMPATIBLE)
        assertThat(c("VARCHAR2", "NUMBER")).isEqualTo(COMPATIBLE)
        assertThat(c("NUMBER", "DATE")).isEqualTo(INCOMPATIBLE)
        assertThat(c("DATE", "NUMBER")).isEqualTo(INCOMPATIBLE)
        assertThat(c("VARCHAR2", "CLOB")).isEqualTo(COMPATIBLE)
        assertThat(c("CLOB", "NUMBER")).isEqualTo(INCOMPATIBLE)
        assertThat(c("CLOB", "BLOB")).isEqualTo(INCOMPATIBLE)
        assertThat(c("BLOB", "VARCHAR2")).isEqualTo(INCOMPATIBLE)
        assertThat(c("RAW", "VARCHAR2")).isEqualTo(COMPATIBLE)
        assertThat(c("NUMBER", "RAW")).isEqualTo(INCOMPATIBLE)
        assertThat(c("VARCHAR2", "RAW")).isEqualTo(COMPATIBLE)
        assertThat(c("RAW", "NUMBER")).isEqualTo(INCOMPATIBLE)
        assertThat(c("TIMESTAMP", "INTERVAL")).isEqualTo(INCOMPATIBLE)
        assertThat(c("INTERVAL", "DATE")).isEqualTo(INCOMPATIBLE)
        assertThat(c("INTERVAL", "INTERVAL")).isEqualTo(UNKNOWN)
        assertThat(c("BOOLEAN", "NUMBER")).isEqualTo(ENVIRONMENT_DEPENDENT)
        assertThat(c("VARCHAR2", "BOOLEAN")).isEqualTo(ENVIRONMENT_DEPENDENT)
        assertThat(c("BOOLEAN", "DATE")).isEqualTo(INCOMPATIBLE)
        assertThat(c("JSON", "VARCHAR2")).isEqualTo(INCOMPATIBLE)
        assertThat(c("VARCHAR2", "JSON")).isEqualTo(INCOMPATIBLE)
        assertThat(c("ROWID", "VARCHAR2")).isEqualTo(COMPATIBLE)
        assertThat(c("NUMBER", "ROWID")).isEqualTo(INCOMPATIBLE)
        assertThat(DatatypeFamilies.compatibility(COMPOSITE, NUMERIC)).isEqualTo(INCOMPATIBLE)
        assertThat(DatatypeFamilies.compatibility(CHARACTER, COMPOSITE)).isEqualTo(INCOMPATIBLE)
        assertThat(DatatypeFamilies.compatibility(COMPOSITE, COMPOSITE)).isEqualTo(UNKNOWN)
    }

    @Test
    fun unclassifiedSidesAreUnknown() {
        assertThat(DatatypeFamilies.compatibility(null, NUMERIC)).isEqualTo(UNKNOWN)
        assertThat(DatatypeFamilies.compatibility(DATETIME, null)).isEqualTo(UNKNOWN)
        assertThat(DatatypeFamilies.compatibility(null, null)).isEqualTo(UNKNOWN)
    }

    @Test
    fun onlyExactUnquotedBuiltInNamesClassify() {
        assertThat(DatatypeFamilies.ofName(name("number"))).isEqualTo(NUMERIC)
        assertThat(DatatypeFamilies.ofName(name("\"NUMBER\""))).isNull()
        assertThat(DatatypeFamilies.ofName(name("sys", "number"))).isNull()
        assertThat(DatatypeFamilies.ofName(name("p", "varchar2"))).isNull()
        listOf("LONG", "BFILE", "XMLTYPE", "SYS_REFCURSOR", "ANYDATA", "MLSLABEL", "my_number", "NUMBER_T").forEach {
            assertThat(DatatypeFamilies.ofName(name(it))).describedAs(it).isNull()
        }
    }

    private fun ext(type: String, owner: String? = null) =
        DatatypeFamilies.ofExternal(ProjectExternalDatatype.of(type, owner, null, null, null, null))

    @Test
    fun dictionaryDatatypesClassifyByBaseWord() {
        assertThat(ext("NUMBER")).isEqualTo(NUMERIC)
        assertThat(ext("FLOAT")).isEqualTo(NUMERIC)
        assertThat(ext("VARCHAR2")).isEqualTo(CHARACTER)
        assertThat(ext("TIMESTAMP(6)")).isEqualTo(DATETIME)
        assertThat(ext("TIMESTAMP(6) WITH TIME ZONE")).isEqualTo(DATETIME)
        assertThat(ext("TIMESTAMP(6) WITH LOCAL TIME ZONE")).isEqualTo(DATETIME)
        assertThat(ext("INTERVAL DAY(2) TO SECOND(6)")).isEqualTo(INTERVAL)
        assertThat(ext("INTERVAL YEAR(2) TO MONTH")).isEqualTo(INTERVAL)
        assertThat(ext("RAW")).isEqualTo(RAW)
        assertThat(ext("UROWID")).isEqualTo(ROWID)
        assertThat(ext("LONG")).isNull()
        assertThat(ext("LONG RAW")).isNull()
        assertThat(ext("BFILE")).isNull()
        assertThat(ext("XMLTYPE", "PUBLIC")).isNull()
        assertThat(ext("NUMBER", "HR")).isNull()
        assertThat(ext("EMP_T", "HR")).isNull()
    }

    @Test
    fun onlyKnownTextSpellingsClassify() {
        listOf("TIMESTAMP SOMETHING", "INTERVAL CUSTOM", "TIMESTAMP WITH ZONE", "INTERVAL DAY TO MONTH", "LONG RAW",
            "VARCHAR2(", "NUMBER(10,2) X", "NUMBER(abc)", "\"NUMBER\"", "SYS.NUMBER", "NUMBER-1", "TIMESTAMP(6) WITH TIME ZONE EXTRA").forEach {
            assertThat(DatatypeFamilies.ofTypeName(it)).describedAs(it).isNull()
        }
        assertThat(ext("TIMESTAMP SOMETHING")).isNull()
        assertThat(ext("INTERVAL CUSTOM")).isNull()
        assertThat(DatatypeFamilies.ofTypeName("varchar2(30 char)")).isEqualTo(CHARACTER)
        assertThat(DatatypeFamilies.ofTypeName("NUMBER(*, 0)")).isEqualTo(NUMERIC)
        assertThat(DatatypeFamilies.ofTypeName("double precision")).isEqualTo(NUMERIC)
        assertThat(DatatypeFamilies.ofTypeName("timestamp(3)  with   local time zone")).isEqualTo(DATETIME)
    }

    // Descriptions through the existing source walk and external composition.

    private val hr = ProjectIdentifier.fromSource("HR")
    private val employees = ProjectExternalRelation.of(
        "HR", "EMPLOYEES", true, listOf(
            ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
            ProjectExternalColumn.of("HIRED", 2, ProjectExternalDatatype.of("DATE", null, null, null, null, null)),
            ProjectExternalColumn.of("DOC", 3, ProjectExternalDatatype.of("XMLTYPE", "PUBLIC", null, null, null, null)),
            ProjectExternalColumn.of("NOTE", 4, null)
        )
    )
    private val md = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(employees).build()

    private val spec = """
        CREATE PACKAGE p AS
          SUBTYPE money_t IS NUMBER(10,2);
          SUBTYPE stamp_t IS TIMESTAMP WITH TIME ZONE;
          SUBTYPE a IS b; SUBTYPE b IS a;
          g money_t;
          TYPE rec_t IS RECORD (id NUMBER);
          TYPE tab_t IS TABLE OF NUMBER;
          TYPE cur_t IS REF CURSOR;
          PROCEDURE run(
            n NUMBER, v VARCHAR2, s stamp_t, m money_t, h g%TYPE, r rec_t, t tab_t, c cur_t, cy a,
            ci employees.id%TYPE, cd employees.hired%TYPE, cx employees.doc%TYPE, cn employees.note%TYPE, cm employees.missing%TYPE,
            er employees%ROWTYPE, ur unknown_relation%ROWTYPE, q "NUMBER", u unknown_t, l LONG);
        END p;
    """.trimIndent()

    private fun families(snapshot: ProjectSnapshot, metadata: ProjectExternalMetadata): Map<String, ProjectDatatypeFamily?> {
        val run = snapshot.findDeclarations(ProjectQualifiedName.of("p", "run")).declarations.single() as ProjectSubprogramView
        return run.parameters.associate { p ->
            val d = snapshot.describeExternalType(p.typeRef, ProjectQualifiedName.of("p"), metadata, hr)
            p.name.originalSpelling to DatatypeFamilies.ofDescription(d, null, p.typeRef)
        }
    }

    @Test
    fun conclusiveDescriptionsClassifyAndInconclusiveOnesDoNot() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("p.sql", spec)), false)
        val f = families(snapshot, md)
        assertThat(f).containsEntry("n", NUMERIC).containsEntry("v", CHARACTER).containsEntry("s", DATETIME)
            .containsEntry("m", NUMERIC).containsEntry("h", NUMERIC).containsEntry("r", COMPOSITE).containsEntry("t", COMPOSITE)
            .containsEntry("ci", NUMERIC).containsEntry("cd", DATETIME).containsEntry("er", COMPOSITE)
        // REF CURSOR, cycles, owned or unknown column datatypes, missing columns, quoted or unknown names, LONG.
        listOf("c", "cy", "cx", "cn", "cm", "q", "u", "l").forEach { assertThat(f[it]).describedAs(it).isNull() }
        // %ROWTYPE syntax is a record whatever the relation resolution.
        assertThat(f["ur"]).isEqualTo(COMPOSITE)

        // Without metadata the column anchors are inconclusive; %ROWTYPE still is a record.
        val none = families(snapshot, ProjectExternalMetadata.none())
        listOf("ci", "cd").forEach { assertThat(none[it]).describedAs(it).isNull() }
        assertThat(none["er"]).isEqualTo(COMPOSITE)
        assertThat(none["n"]).isEqualTo(NUMERIC)
        // Partial metadata: the relation is known with partial columns, so a missing column is not covered.
        val partial = ProjectExternalMetadata.builder("partial").relation(
            ProjectExternalRelation.of("HR", "EMPLOYEES", false, listOf(ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, null, null))))
        ).build()
        val p = families(snapshot, partial)
        assertThat(p["ci"]).isEqualTo(NUMERIC)
        assertThat(p["cd"]).isNull()
    }

    @Test
    fun incompleteOrConditionalSourcesNeverClassify() {
        val failing = ProjectSourceInput.ofReader("bad.sql", ProjectSourceReader { throw IllegalStateException("x") })
        val incomplete = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("p.sql", spec), failing), false)
        val f = families(incomplete, md)
        // Built-in names do not depend on project facts; project lookups and column anchors do.
        assertThat(f["n"]).isEqualTo(NUMERIC)
        listOf("m", "h", "r", "t", "ci").forEach { assertThat(f[it]).describedAs(it).isNull() }

        val conditional = """
            CREATE PACKAGE p AS
              ${'$'}IF ${'$'}${'$'}f ${'$'}THEN SUBTYPE money_t IS NUMBER; ${'$'}ELSE SUBTYPE money_t IS DATE; ${'$'}END
              PROCEDURE run(m money_t, n NUMBER);
            END p;
        """.trimIndent()
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("c.sql", conditional)), false)
        val c = families(snapshot, md)
        assertThat(c["m"]).isNull()
        assertThat(c["n"]).isEqualTo(NUMERIC)
    }

    @Test
    fun nameQueriesUseTheRequestNameAndDeclaredTypes() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("p.sql", spec)), false)
        fun byName(vararg n: String) = ProjectQualifiedName.of(*n).let {
            DatatypeFamilies.ofDescription(snapshot.describeExternalType(it, null, md, hr), it, null)
        }
        assertThat(byName("binary_double")).isEqualTo(NUMERIC)
        assertThat(byName("p", "money_t")).isEqualTo(NUMERIC)
        assertThat(byName("p", "rec_t")).isEqualTo(COMPOSITE)
        assertThat(byName("p", "cur_t")).isNull()
        assertThat(byName("p", "missing_t")).isNull()
    }
}
