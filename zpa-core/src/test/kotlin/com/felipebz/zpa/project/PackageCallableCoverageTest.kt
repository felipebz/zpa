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
package com.felipebz.zpa.project

import com.felipebz.zpa.project.CallableCoverageGap.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Callable coverage of retained package specifications, through the real lexer and extractor. */
class PackageCallableCoverageTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun specs(source: String, file: String = "spec.sql") =
        extractor.extract(FileId(file), source).filterIsInstance<PackageDeclaration>()

    private fun coverage(source: String) = specs(source).single().callableCoverage

    private fun facts(source: String) = extractor.extract(FileId("spec.sql"), source).filter { it !is PackageDeclaration }

    @Test
    fun cleanSpecificationIsComplete() {
        val c = coverage(
            """
            CREATE PACKAGE p AS
              g NUMBER; -- PROCEDURE commented_out;
              /* FUNCTION hidden RETURN NUMBER; ${'$'}IF */
              s CONSTANT VARCHAR2(20) := '${'$'}IF x ${'$'}THEN &y';
              PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1);
              FUNCTION f RETURN NUMBER;
              TYPE r IS RECORD (a NUMBER);
            END p;
            """.trimIndent()
        )
        assertThat(c).isEqualTo(PackageCallableCoverage.COMPLETE)
        assertThat(c.complete).isTrue()
    }

    @Test
    fun polymorphicTableFunctionHeadersAreOmittedByName() {
        val c = coverage(
            """
            CREATE PACKAGE skip_col_pkg AS
              FUNCTION skip_col(tab TABLE, col COLUMNS) RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;
              FUNCTION describe(tab IN OUT DBMS_TF.TABLE_T, col DBMS_TF.COLUMNS_T) RETURN DBMS_TF.DESCRIBE_T;
            END skip_col_pkg;
            """.trimIndent()
        )
        assertThat(c.omittedNames).containsExactly("SKIP_COL")
        assertThat(c.unknown).isFalse()
    }

    @Test
    fun retainedOverloadDoesNotHideASkippedOneAndQuotedNamesAreNormalized() {
        val source = """
            CREATE PACKAGE p AS
              FUNCTION run(a NUMBER) RETURN NUMBER;
              FUNCTION run(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p;
              FUNCTION "Mixed"(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p;
              FUNCTION "RUN2"(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p;
            END p;
            """.trimIndent()
        assertThat(facts(source).map { it.qualifiedName().toString() }).containsExactly("p.run")
        val c = coverage(source)
        assertThat(c.omittedNames).containsExactly("Mixed", "RUN", "RUN2")
        assertThat(c.unknown).isFalse()
    }

    @Test
    fun unidentifiableHeadersAreUnknown() {
        assertThat(coverage("CREATE PACKAGE p AS PROCEDURE ; END p;").gaps).containsExactly(UNIDENTIFIED_CALLABLE_HEADER)
        assertThat(coverage("CREATE PACKAGE p AS PROCEDURE 1x; END p;").gaps).containsExactly(UNIDENTIFIED_CALLABLE_HEADER)
        assertThat(coverage("CREATE PACKAGE p AS FUNCTION ; END p;").gaps).containsExactly(AMBIGUOUS_FUNCTION_DECLARATION)
    }

    @Test
    fun conditionalCompilationMakesCoverageUnknown() {
        val source = "CREATE PACKAGE p AS\r\n${'$'}IF DBMS_DB_VERSION.VER_LE_12 ${'$'}THEN\r\n  g NUMBER;\r\n${'$'}ELSE\r\n" +
            "  PROCEDURE q;\r\n${'$'}END\r\nEND p;\r\n"
        assertThat(facts(source).map { it.qualifiedName().toString() }).containsExactly("p.g") // q is never lexed
        assertThat(coverage(source).gaps).containsExactly(CONDITIONAL_COMPILATION)
        // adjacent to the first and to the last declaration token
        assertThat(coverage("CREATE PACKAGE p AS ${'$'}IF ${'$'}${'$'}x ${'$'}THEN PROCEDURE a; ${'$'}END END p;").gaps)
            .containsExactly(CONDITIONAL_COMPILATION)
        assertThat(coverage("CREATE PACKAGE p AS PROCEDURE a;${'$'}IF ${'$'}${'$'}x ${'$'}THEN ${'$'}END\nEND p;").gaps)
            .containsExactly(CONDITIONAL_COMPILATION)
    }

    @Test
    fun substitutionsCountOnlyInsideTheDeclarationRegion() {
        assertThat(coverage("CREATE PACKAGE p AS\n  PROCEDURE a;\n  &extra_members\n  PROCEDURE b;\nEND p;").gaps).containsExactly(SUBSTITUTION)
        assertThat(coverage("CREATE OR REPLACE PACKAGE p &start_ge_8_1 AUTHID CURRENT_USER &end_ge_8_1 IS\n  PROCEDURE a;\nEND p;"))
            .isEqualTo(PackageCallableCoverage.COMPLETE)
    }

    @Test
    fun droppedNonCallablesDoNotCreateNames() {
        val c = coverage(
            """
            CREATE PACKAGE p AS
              v weird type here;
              CURSOR c IS SELECT 1 FROM dual;
              PRAGMA SERIALLY_REUSABLE;
              e EXCEPTION;
              TYPE t IS OBJECT (a NUMBER);
            END p;
            """.trimIndent()
        )
        assertThat(c).isEqualTo(PackageCallableCoverage.COMPLETE)
    }

    @Test
    fun functionLedDeclarationsAreClassifiedConservatively() {
        // a retained variable named function is not a gap
        assertThat(coverage("CREATE PACKAGE p AS function NUMBER; END p;")).isEqualTo(PackageCallableCoverage.COMPLETE)
        // a dropped one that is not a callable shape cannot be excluded
        assertThat(coverage("CREATE PACKAGE p AS function weird_t x y; END p;").gaps).containsExactly(AMBIGUOUS_FUNCTION_DECLARATION)
    }

    @Test
    fun swallowedHeadersAreUnknownAndRecoveryIsUnchanged() {
        // the existing parsers retain these declarations although a function header follows without a `;`
        for (swallowed in listOf(
            "CREATE PACKAGE p AS TYPE t IS RECORD (a NUMBER) FUNCTION f RETURN NUMBER; PROCEDURE ok; END p;",
            "CREATE PACKAGE p AS t NUMBER := 1 FUNCTION f RETURN NUMBER; PROCEDURE ok; END p;"
        )) {
            assertThat(facts(swallowed).map { it.qualifiedName().toString() }).containsExactly("p.t", "p.ok")
            assertThat(coverage(swallowed).gaps).containsExactly(SWALLOWED_CALLABLE_HEADER)
        }
        // a dropped non-callable declaration that contains a callable keyword
        assertThat(coverage("CREATE PACKAGE p AS CURSOR c RETURN FUNCTION x; END p;").gaps).containsExactly(SWALLOWED_CALLABLE_HEADER)
        // missing `;` before a reserved PROCEDURE: the broken header is omitted by name, the next one is still retained
        val recovered = "CREATE PACKAGE p AS PROCEDURE broken(a NUMBER) PROCEDURE ok; END p;"
        assertThat(facts(recovered).map { it.qualifiedName().toString() }).containsExactly("p.ok")
        assertThat(coverage(recovered).omittedNames).containsExactly("BROKEN")
    }

    @Test
    fun droppedCallableHeadingThatAbsorbsTheNextHeadingMakesCoverageUnknown() {
        // `f` misses its `;`, so the dropped declaration also contains the heading of `g`, which has no fact.
        val swallowing = "CREATE PACKAGE p AS FUNCTION f RETURN NUMBER FUNCTION g RETURN NUMBER; PROCEDURE ok; END p;"
        assertThat(facts(swallowing).map { it.qualifiedName().toString() }).containsExactly("p.ok")
        assertThat(coverage(swallowing).omittedNames).containsExactly("F")
        assertThat(coverage(swallowing).gaps).containsExactly(SWALLOWED_CALLABLE_HEADER)
        // Valid neighbours and clauses with nested callable keywords are unaffected.
        assertThat(coverage("CREATE PACKAGE p AS FUNCTION f(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p; " +
            "FUNCTION g RETURN NUMBER ACCESSIBLE BY (FUNCTION caller); PROCEDURE ok; END p;"))
            .isEqualTo(PackageCallableCoverage(listOf("F"), emptyList()))
        assertThat(coverage("CREATE PACKAGE p AS PROCEDURE broken(a NUMBER, b IN OUT); PROCEDURE ok; END p;"))
            .isEqualTo(PackageCallableCoverage(listOf("BROKEN"), emptyList()))
    }

    @Test
    fun unretainedUnitsCarryNoMarker() {
        assertThat(specs("CREATE OR REPLACE PACKAGE w WRAPPED\na000000\nabcd\n/\n")).isEmpty()
        assertThat(specs("CREATE PACKAGE u AS PROCEDURE a;")).isEmpty()
    }

    @Test
    fun duplicateSpecificationsKeepIndependentMarkers() {
        val source = "CREATE PACKAGE p AS PROCEDURE a; END p;\n/\nCREATE PACKAGE p AS FUNCTION a(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p; END p;\n/\n" +
            "CREATE PACKAGE q AS ${'$'}IF ${'$'}${'$'}x ${'$'}THEN PROCEDURE a; ${'$'}END END q;\n/\n"
        val markers = specs(source).map { it.name.toString() to it.callableCoverage }
        assertThat(markers).containsExactly(
            "p" to PackageCallableCoverage.COMPLETE,
            "p" to PackageCallableCoverage(listOf("A"), emptyList()),
            "q" to PackageCallableCoverage(emptyList(), listOf(CONDITIONAL_COMPILATION))
        )
    }

    @Test
    fun preparationOrderAndConcurrencyDoNotChangeMarkers() {
        val sources = (1..12).map { i ->
            "f$i.sql" to "CREATE PACKAGE p$i AS FUNCTION a(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p; ${if (i % 3 == 0) "\$IF \$\$x \$THEN \$END" else ""} PROCEDURE b; END p$i;"
        }
        fun markers(list: List<Pair<String, String>>, concurrent: Boolean): Map<String, PackageCallableCoverage> {
            val result = com.felipebz.zpa.project.ProjectIndexPreparation(extractor)
                .prepare(list.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent)
            return result.index.declarations.filterIsInstance<PackageDeclaration>()
                .associate { "${it.fileId.value}#${it.name}" to it.callableCoverage }
        }
        val serial = markers(sources, false)
        assertThat(serial).hasSize(12)
        assertThat(markers(sources, true)).isEqualTo(serial)
        assertThat(markers(sources.reversed(), true)).isEqualTo(serial)
        assertThat(serial.getValue("f3.sql#p3").gaps).containsExactly(CONDITIONAL_COMPILATION)
        assertThat(serial.getValue("f1.sql#p1").omittedNames).containsExactly("A")
    }

    @Test
    fun markersAreImmutableAndDetached() {
        val names = mutableListOf("A")
        val gaps = mutableListOf(SUBSTITUTION)
        val marker = PackageCallableCoverage(names, gaps)
        names += "B"
        gaps += CONDITIONAL_COMPILATION
        assertThat(marker.omittedNames).containsExactly("A")
        assertThat(marker.gaps).containsExactly(SUBSTITUTION)
        assertThatThrownBy { (marker.omittedNames as MutableSet<String>).add("C") }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (marker.gaps as MutableSet<CallableCoverageGap>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(marker).isEqualTo(PackageCallableCoverage(listOf("A"), listOf(SUBSTITUTION)))
            .hasSameHashCodeAs(PackageCallableCoverage(listOf("A"), listOf(SUBSTITUTION)))
        // only strings and enum constants are reachable from a marker
        assertThat(PackageCallableCoverage::class.java.declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.type })
            .allMatch { it == Set::class.java }
    }
}
