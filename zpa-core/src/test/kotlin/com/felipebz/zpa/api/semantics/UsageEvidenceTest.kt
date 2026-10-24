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
