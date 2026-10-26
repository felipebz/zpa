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
package com.felipebz.zpa.tooling

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.symbols.local.LocalResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** The declared project types the overview and the node inspection show, from the local declarations. */
class DeclaredProjectTypeTest {

    private val types = "CREATE PACKAGE pk AS TYPE r IS RECORD (fld NUMBER); END pk;\n/\n" +
        "CREATE TYPE t AS OBJECT (a NUMBER);\n/\n"

    private val source = """
        |CREATE FUNCTION f(o t) RETURN pk.r IS
        |  v pk.r;
        |  ${'$'}IF ${'$'}${'$'}extra ${'$'}THEN
        |  w pk.r;
        |  ${'$'}END
        |BEGIN
        |  v.fld := o.a;
        |  w.fld := 1;
        |  RETURN v;
        |END;
        |/
        |""".trimMargin()

    private fun analyze(): Pair<SemanticInspectionResult, AstNode> {
        val sources = listOf("types.sql" to types, "use.sql" to source)
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(PlSqlConfiguration(StandardCharsets.UTF_8)))
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8)).parse(source)
        return SemanticInspectionService().analyze(root, FileId("use.sql"), context, source) to root
    }

    private fun name(root: AstNode, line: Int, column: Int) =
        root.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).single { it.token.line == line && it.token.column == column }

    @Test
    fun theOverviewListsEachLocalDeclarationWithADeclaredProjectType() {
        val (result, _) = analyze()
        fun find(node: ProjectOverviewNode): ProjectOverviewNode? =
            if (node.label.startsWith("Variables typed by project types")) node else node.children.firstNotNullOfOrNull(::find)
        assertThat(find(result.projectOverview)!!.children.map { "${it.sourceRange!!.startLine}:${it.sourceRange!!.startColumn} ${it.label}" })
            .containsExactlyInAnyOrder(
                "1:16 function f: pk.r (PACKAGE_TYPE)",
                "1:18 parameter o: t (STANDALONE_TYPE)",
                "2:2 variable v: pk.r (PACKAGE_TYPE)",
                "4:2 variable w: pk.r (PACKAGE_TYPE)"
            )
    }

    @Test
    fun theInspectionShowsTheDeclaredTypeOfTheNamedDeclaration() {
        val (result, root) = analyze()
        fun type(line: Int, column: Int) = result.inspect(name(root, line, column)).declaredProjectType?.qualifiedName
        // A variable typed by a package record, a parameter typed by a schema object type, the function's result.
        assertThat(type(7, 2)).isEqualTo("pk.r")
        assertThat(type(7, 11)).isEqualTo("t")
        assertThat(type(1, 16)).isEqualTo("pk.r")
        // In the block made incomplete by the conditional declarations, w's only candidate is the declaration in the ${'$'}IF.
        assertThat(result.localSemanticFacts!!.firstOutcome(8, 2)).isInstanceOf(LocalResolution.Incomplete::class.java)
        assertThat(type(8, 2)).isEqualTo("pk.r")
    }

    @Test
    fun bothAgreeWithTheLocalFacts() {
        val (result, _) = analyze()
        val facts = result.localSemanticFacts!!
        val recorded = facts.model.declarations.mapNotNull { d -> facts.declaredType(d)?.let { "${d.nameRange.startLine}:${d.nameRange.startColumn} ${it.qualifiedName}" } }
        assertThat(recorded).containsExactlyInAnyOrder("1:16 pk.r", "1:18 t", "2:2 pk.r", "4:2 pk.r")
    }

    @Test
    fun onlyTheNameItselfAnswers() {
        val types = "CREATE PACKAGE pk AS TYPE r IS RECORD (fld NUMBER); FUNCTION g RETURN r; END pk;\n/\n" +
            "CREATE TYPE t AS OBJECT (a NUMBER);\n/\n"
        val use = "CREATE PROCEDURE p(o t) IS\n  v pk.r;\n  n NUMBER;\nBEGIN\n  v.fld := o.a;\n  n := v.fld + 1;\n  v := pk.g;\nEND;\n/\n"
        val sources = listOf("types.sql" to types, "use.sql" to use)
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(PlSqlConfiguration(StandardCharsets.UTF_8)))
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8)).parse(use)
        val result = SemanticInspectionService().analyze(root, FileId("use.sql"), context, use)
        fun type(node: AstNode) = result.inspect(node).declaredProjectType?.qualifiedName
        fun at(line: Int, column: Int) = root.descendants().filter { it.hasToken() && it.token.line == line && it.token.column == column }

        // The name's own nodes: v at its declaration, v and o in the body, from the VARIABLE_NAME down to the token.
        val names = at(2, 2).filter { it.lastToken === it.token } + at(5, 2).filter { it.lastToken === it.token } +
            at(5, 11).filter { it.lastToken === it.token }
        assertThat(names.map { "${it.token.line}:${it.token.column} ${it.type} ${type(it)}" }).containsExactlyInAnyOrder(
            "2:2 IDENTIFIER_NAME pk.r", "2:2 IDENTIFIER pk.r",
            "5:2 VARIABLE_NAME pk.r", "5:2 IDENTIFIER_NAME pk.r", "5:2 IDENTIFIER pk.r",
            "5:11 VARIABLE_NAME t", "5:11 IDENTIFIER_NAME t", "5:11 IDENTIFIER t"
        )
        // Larger nodes starting with a name have a type of their own.
        val larger = listOf(
            PlSqlGrammar.DECLARE_SECTION, PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.STATEMENTS,
            PlSqlGrammar.ASSIGNMENT_STATEMENT, PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.ADDITIVE_EXPRESSION
        ).flatMap { root.getDescendants(it) }
        assertThat(larger.map { it.type.toString() }).contains("DECLARE_SECTION", "VARIABLE_DECLARATION", "STATEMENTS", "ADDITIVE_EXPRESSION")
        assertThat(larger.filter { it.type === PlSqlGrammar.ASSIGNMENT_STATEMENT }).hasSize(3)
        assertThat(larger.map { "${it.type} ${type(it)}" }).allMatch { it.endsWith(" null") }
    }

    private fun AstNode.descendants(): List<AstNode> = children.flatMap { listOf(it) + it.descendants() }
}
