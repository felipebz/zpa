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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class UsageEvidenceTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** `NAME: evidence` for each variable and exception declaration of [text], analyzed with [others] as the project. */
    private fun evidence(text: String, vararg others: Pair<String, String>, prepared: Boolean = true): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    /** `NAME: evidence` for each parameter and cursor parameter declaration of [text], with no project. */
    private fun parameters(text: String): List<String> {
        val project = ProjectAnalysisContext.NOT_PREPARED
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_PARAMETER_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    private fun cursors(text: String): List<String> {
        val project = ProjectAnalysisContext.NOT_PREPARED
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.CURSOR_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    @Test
    fun usedUnusedAndUnknownWithTheirReasons() {
        val text = "declare\n  used number;\n  unused number;\n  in_sql number;\n  e exception;\n  n number;\nbegin\n" +
            "  used := 1;\n  select count(in_sql) into n from some_table;\n  raise e;\nend;\n/\n"
        assertThat(evidence(text)).containsExactly(
            "USED: Used",
            "UNUSED: Unused",
            "IN_SQL: Unknown[SQL_NAME_MAY_BE_COLUMN]",
            "E: Used",
            "N: Used"
        )
    }

    @Test
    fun textTheAnalysisCannotSeeIsNeverAProvenAbsence() {
        val conditional = "declare\n  v number;\nbegin\n  \$if \$\$x \$then null; \$else v := 1; \$end\nend;\n/\n"
        assertThat(evidence(conditional)).containsExactly("V: Unknown[CONDITIONAL_COMPILATION]")
        val substitution = "declare\n  w number;\n  n number;\nbegin\n  n := 1 &plus_w;\nend;\n/\n"
        assertThat(evidence(substitution)).containsExactly("W: Unknown[SUBSTITUTION]", "N: Unknown[SUBSTITUTION]")
    }

    @Test
    fun aReferenceBeforeTheDeclarationIsAUse() {
        assertThat(evidence("declare\n  a number := b;\n  b number := 1;\nbegin\n  null;\nend;\n/\n")).containsExactly("A: Unused", "B: Used")
    }

    @Test
    fun parametersAndCursorParameters() {
        val text = "create procedure p(used number, unused number, named number) is\n" +
            "  cursor c(in_query number, unused_c number) is select count(*) from t where in_query = 1;\n" +
            "begin\n  print(p.used);\n  q(named => 1);\nend;\n/\n" +
            "create procedure ext(x number) as language java name 'A.run(int)';\n/\n" +
            "create package s as\n  procedure h(hp number);\nend;\n/\n"
        assertThat(parameters(text)).containsExactly(
            "USED: Used",
            "UNUSED: Unused",
            "NAMED: Unused",
            "IN_QUERY: Unknown[SQL_NAME_MAY_BE_COLUMN]",
            "UNUSED_C: Unused",
            "X: Unavailable(NOT_A_COVERED_DECLARATION)",
            "HP: Unavailable(NOT_A_COVERED_DECLARATION)"
        )
    }

    @Test
    fun cursors() {
        val text = "declare\n  cursor c_rowtype is select 1 a from dual;\n  r c_rowtype%rowtype;\n" +
            "  cursor c_attribute is select 1 from dual;\n  cursor dummy is select 1 from dual;\n" +
            "  cursor c_unused is select 1 from dual;\n  v varchar2(1);\nbegin\n" +
            "  if c_attribute%isopen then null; end if;\n  select dummy into v from dual;\nend;\n/\n"
        assertThat(cursors(text)).containsExactly(
            "C_ROWTYPE: Used",
            "C_ATTRIBUTE: Used",
            "DUMMY: Unknown[UNSUPPORTED_REFERENCE]",
            "C_UNUSED: Unused"
        )
    }

    /** `NAME: hiding evidence` for each variable and exception declaration of [text]. */
    private fun hiding(text: String, vararg others: Pair<String, String>, prepared: Boolean = true): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.hidingEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    @Test
    fun hidingFollowsOracleVisibility() {
        val text = "create procedure p(par number) is\n  v number;\nbegin\n  for i in 1..2 loop\n    declare\n" +
            "      v number;\n      par number;\n      i number;\n      fresh number;\n    begin\n      null;\n    end;\n" +
            "  end loop;\nend;\n/\ndeclare\n  d number;\n  d number;\nbegin\n  null;\nend;\n/\n"
        assertThat(hiding(text)).containsExactly(
            "V: HidesNothing",
            "V: Hides(2:2)",
            "PAR: Hides(1:19)",
            "I: Hides(4:6)",
            "FRESH: HidesNothing",
            "D: HidesNothing",
            "D: HidesNothing"
        )
    }

    @Test
    fun aPairTheAnalysisCannotFullySeeIsUnknown() {
        val text = "declare\n  y number;\nbegin\n  \$if \$\$a \$then null; \$end\n  declare\n    y number;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(hiding(text)).containsExactly("Y: HidesNothing", "Y: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun aHiddenDeclarationNamesItsFileTheSameWayOnEveryMachine() {
        fun name(fileId: String?) = HiddenDeclaration(2, 2, fileId, null).fileName
        assertThat(name("counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("src/db/counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("file:///home/runner/work/my%20app/src/counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("C:\\work\\app\\counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name(null)).isNull()
    }

    @Test
    fun aSpecificationVariableInAnotherFile() {
        val spec = "create package p as\n  counter number;\n  procedure run;\nend;\n/\n"
        val body = "create package body p as\n  procedure run is\n    counter number;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(hiding(body, "s.sql" to spec)).containsExactly("COUNTER: Hides(s.sql:2:2)")
        assertThat(hiding(body, prepared = false)).containsExactly("COUNTER: Unknown[DECLARATION_OUTSIDE_FILE]")
        // A body declaration is a duplicate of the specification's, not hiding: the same answer with or without the project.
        val duplicate = "create package body p as\n  counter number;\n  procedure run is begin null; end;\nend;\n/\n"
        assertThat(hiding(duplicate, "s.sql" to spec)).containsExactly("COUNTER: HidesNothing")
        assertThat(hiding(duplicate, prepared = false)).containsExactly("COUNTER: HidesNothing")
    }

    @Test
    fun aSpecificationVariableIsVisibleOutsideTheFile() {
        assertThat(evidence("create package p as\n  g number;\nend;\n/\n")).containsExactly("G: Unknown[VISIBLE_OUTSIDE_FILE]")
    }

    @Test
    fun theProjectCoverageMakesAUseInABodyCertain() {
        val spec = "create package p as\n  procedure run;\nend;\n/\n"
        val body = "create package body p as\n  counter number;\n  procedure run is begin counter := 1; end;\nend;\n/\n"
        // Without the project, the specification in another file may declare the name too.
        assertThat(evidence(body, prepared = false)).containsExactly("COUNTER: Unknown[DECLARATION_OUTSIDE_FILE]")
        assertThat(evidence(body, "s.sql" to spec)).containsExactly("COUNTER: Used")
    }

    @Test
    fun noEvidenceWithoutTheLocalAnalysisOrForAnotherNode() {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse("declare\n  v number;\nbegin\n  null;\nend;\n/\n")
        val declaration = root.getFirstDescendant(PlSqlGrammar.VARIABLE_DECLARATION)!!
        val context = PlSqlVisitorContext(root, Source("u.sql", ""), null)
        assertThat(context.usageEvidence(declaration)).isEqualTo(UsageEvidence.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))

        val rows = ArrayList<UsageEvidence>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                rows += this.context.usageEvidence(node.getFirstDescendant(PlSqlGrammar.STATEMENTS_SECTION)!!)
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", "declare\n  v number;\nbegin\n  null;\nend;\n/\n"), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        assertThat(rows).containsExactly(UsageEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION))
    }
}
