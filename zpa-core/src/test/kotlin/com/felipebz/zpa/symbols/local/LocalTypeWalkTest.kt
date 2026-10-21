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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** The datatype evidence of local values: the walk from a declaration's written type to its terminal. */
@OptIn(ZpaExperimentalApi::class)
class LocalTypeWalkTest {

    private val packages = "create package pkg_other as\n  subtype amt_t is date;\n  g_amt amt_t;\nend;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pkg_other.sql", packages)), false)

    private fun model(source: String, fileId: String = "walk.sql"): LocalSemanticModel {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        return LocalSemanticModelBuilder.build(root, FileId(fileId), source)
    }

    /** The evidence of the [occurrence]-th value named [name], with the kinds and names of the declarations followed. */
    private fun evidence(source: String, name: String = "v", occurrence: Int = 0, snapshot: ProjectSnapshot? = this.snapshot, fileId: String = "walk.sql"): String {
        val model = model(source, fileId)
        val declaration = model.declarations.filter { it.name.lookupName == name.uppercase() }[occurrence]
        val evidence = LocalTypeWalk(model, snapshot).value(declaration.id)
        val followed = evidence.followed.joinToString(" > ") { model.declaration(it).let { d -> "${d.kind} ${d.name.originalSpelling}" } }
        return "${evidence.terminal} ${evidence.argumentType} [$followed]" + (evidence.unknownReason?.let { " $it" } ?: "")
    }

    private fun block(declarations: String) = "declare\n$declarations\nbegin\n  null;\nend;\n/\n"

    @Test
    fun subtypesAreFollowedToTheirWrittenType() {
        assertThat(evidence(block("  subtype a is date;\n  subtype b is a;\n  v b;")))
            .isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE v > SUBTYPE b > SUBTYPE a]")
        // A constrained base keeps its name; the constraint is not evidence.
        assertThat(evidence(block("  subtype s is varchar2(10);\n  v s;"))).isEqualTo("BUILT_IN BUILT_IN CHARACTER [VARIABLE v > SUBTYPE s]")
    }

    @Test
    fun aLocalSubtypeNamedLikeAPredefinedTypeIsTheLocalType() {
        assertThat(evidence(block("  subtype number is date;\n  v number;"))).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE v > SUBTYPE number]")
        // Without it the name is the predefined type.
        assertThat(evidence(block("  v number;"))).isEqualTo("BUILT_IN BUILT_IN NUMERIC [VARIABLE v]")
    }

    @Test
    fun aTypeKeepsTheMeaningItHadWhereItIsWritten() {
        // The outer v keeps the outer subtype, whatever an inner block declares later.
        val nested = "declare\n  subtype t is date;\n  v t;\nbegin\n  declare\n    subtype t is number;\n    w t;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(evidence(nested)).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE v > SUBTYPE t]")
        assertThat(evidence(nested, "w")).isEqualTo("BUILT_IN BUILT_IN NUMERIC [VARIABLE w > SUBTYPE t]")
        // A subtype declared later in the same region is an order error (PLS-00311): no evidence.
        assertThat(evidence(block("  v t;\n  subtype t is date;"))).startsWith("UNKNOWN UNKNOWN [VARIABLE v] DeclaredLater")
    }

    @Test
    fun localAnchorsWalkTheAnchoredDeclaration() {
        assertThat(evidence(block("  x date;\n  v x%type;"))).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE v > VARIABLE x]")
        // A record field.
        assertThat(evidence(block("  type r_t is record (a date);\n  r r_t;\n  v r.a%type;"))).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE v]")
        // A parameter cannot anchor %TYPE (PLS-00206).
        assertThat(evidence("create procedure p(x date) is\n  v x%type;\nbegin\n  null;\nend;\n/\n")).startsWith("UNKNOWN UNKNOWN [VARIABLE v] WrongKind")
        // A cursor's %ROWTYPE is a row.
        assertThat(evidence(block("  cursor c is select 1 a from dual;\n  v c%rowtype;"))).isEqualTo("COMPOSITE COMPOSITE COMPOSITE [VARIABLE v > CURSOR c]")
    }

    @Test
    fun anAnchorOrQualifiedTypeOutsideTheFileIsDescribedByTheProject() {
        assertThat(evidence(block("  v pkg_other.g_amt%type;"))).isEqualTo("PROJECT DESCRIBED DATETIME [VARIABLE v]")
        assertThat(evidence(block("  v pkg_other.amt_t;"))).isEqualTo("PROJECT DESCRIBED DATETIME [VARIABLE v]")
        // A table's %ROWTYPE is a row whatever the project knows.
        assertThat(evidence(block("  v emp%rowtype;"))).isEqualTo("PROJECT COMPOSITE COMPOSITE [VARIABLE v]")
        // Without a snapshot nothing outside the file is described.
        assertThat(evidence(block("  v pkg_other.amt_t;"), snapshot = null)).isEqualTo("UNKNOWN UNKNOWN [VARIABLE v] no project snapshot")
    }

    @Test
    fun recordsAndCollectionsAreComposite() {
        assertThat(evidence(block("  type r_t is record (a date);\n  v r_t;"))).isEqualTo("COMPOSITE COMPOSITE COMPOSITE [VARIABLE v > RECORD_TYPE r_t]")
        assertThat(evidence(block("  type n_t is table of number;\n  v n_t;"))).isEqualTo("COMPOSITE COMPOSITE COMPOSITE [VARIABLE v > COLLECTION_TYPE n_t]")
        assertThat(evidence(block("  type c_t is ref cursor;\n  v c_t;"))).isEqualTo("COMPOSITE COMPOSITE COMPOSITE [VARIABLE v > REF_CURSOR_TYPE c_t]")
    }

    @Test
    fun cyclesAndWrongKindsAreUnknown() {
        // The order rule rejects every self or mutual reference before the walk could loop.
        assertThat(evidence(block("  subtype a is b;\n  subtype b is a;\n  v a;"))).startsWith("UNKNOWN UNKNOWN [VARIABLE v > SUBTYPE a] DeclaredLater")
        assertThat(evidence(block("  subtype c is c;\n  v c;"))).startsWith("UNKNOWN UNKNOWN [VARIABLE v > SUBTYPE c] DeclaredLater")
        assertThat(evidence(block("  v v%type;"))).startsWith("UNKNOWN UNKNOWN [VARIABLE v] DeclaredLater")
        // A variable used as a type (PLS-00488).
        assertThat(evidence(block("  x number;\n  v x;"))).startsWith("UNKNOWN UNKNOWN [VARIABLE v] WrongKind")
    }

    @Test
    fun anIncompleteRegionOnTheWayGivesUnknown() {
        val source = "declare\n  subtype t is date;\nbegin\n  declare\n    \$IF \$\$x \$THEN subtype t is number; \$END\n    v t;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(evidence(source)).startsWith("UNKNOWN UNKNOWN [VARIABLE v] Incomplete")
        // A loop index typed by its iteration control has no written type.
        assertThat(evidence("begin\n  for i in 1..2 loop null; end loop;\nend;\n/\n", "i")).isEqualTo("UNKNOWN UNKNOWN [LOOP_INDEX i] typed by its iteration control")
    }

    @Test
    fun qualifiedTypesOfTheComparisonAreDescribedByTheProject() {
        fun described(name: String, variable: String, line: Int): String {
            val text = java.io.File("src/test/resources/local-semantic/sources/$name.sql").readText()
            val prepared = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("$name.sql", text)), false)
            val model = model(text, "$name.sql")
            val declaration = model.declarations.single { it.name.lookupName == variable.uppercase() && it.range.startLine == line }
            return LocalTypeWalk(model, prepared).value(declaration.id).toString()
        }
        // A package subtype named like a predefined type, written qualified: Oracle's DATE, NUMBER, NUMBER, DATE.
        assertThat(described("package_subtypes_named_like_predefined_types", "v", 41)).isEqualTo("PROJECT DESCRIBED DATETIME")
        assertThat(described("predefined_type_named_subtypes", "a", 27)).isEqualTo("PROJECT DESCRIBED NUMERIC")
        assertThat(described("predefined_type_named_subtypes", "b", 28)).isEqualTo("PROJECT DESCRIBED NUMERIC")
        assertThat(described("predefined_type_named_subtypes", "c", 29)).isEqualTo("PROJECT DESCRIBED DATETIME")
        // A package record type.
        assertThat(described("local_named_like_its_procedure", "rv", 13)).isEqualTo("PROJECT DESCRIBED COMPOSITE")
    }

    @Test
    fun aSpecificationValueUsesTheProjectFactWhenPrepared() {
        // A package value whose specification is in the file: with a prepared snapshot of that file the project's own
        // fact, matched by range, describes it; without one the specification is walked locally.
        val source = "create package pk as\n  subtype money_t is date;\n  g money_t;\nend;\n/\n"
        val prepared = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pk.sql", source)), false)
        assertThat(evidence(source, "g", snapshot = prepared, fileId = "pk.sql")).isEqualTo("PROJECT DESCRIBED DATETIME [VARIABLE g]")
        assertThat(evidence(source, "g", snapshot = null, fileId = "pk.sql")).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE g > SUBTYPE money_t]")
        // A package subtype named like a predefined type: the project gives the predefined family, which narrowing
        // checks against the package's members and treats as unknown; the local walk gives the package subtype.
        val shadow = "create package pk as\n  subtype integer is date;\n  g integer;\nend;\n/\n"
        val preparedShadow = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pk.sql", shadow)), false)
        assertThat(evidence(shadow, "g", snapshot = preparedShadow, fileId = "pk.sql")).isEqualTo("PROJECT DESCRIBED NUMERIC [VARIABLE g]")
        assertThat(evidence(shadow, "g", snapshot = null, fileId = "pk.sql")).isEqualTo("BUILT_IN BUILT_IN DATETIME [VARIABLE g > SUBTYPE integer]")
    }
}
