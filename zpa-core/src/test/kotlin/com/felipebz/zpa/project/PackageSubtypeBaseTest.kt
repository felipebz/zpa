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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectSubtypeView
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** The base datatype of package subtypes ends before a top-level nullability constraint or `RANGE`. */
class PackageSubtypeBaseTest {
    private val fileId = FileId("fixture/subtypes.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun subtypes(source: String) = extractor.extract(fileId, source).filterIsInstance<PackageSubtypeDeclaration>()
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
    private fun spec(vararg declarations: String) = "CREATE PACKAGE p AS\n${declarations.joinToString("\n") { "  $it" }}\nEND p;"

    private data class Base(val kind: String, val name: String, val range: SourceRange)

    private fun range(node: AstNode) = SourceRange(fileId, node.token.line, node.token.column, node.lastToken.endLine, node.lastToken.endColumn)

    private fun baseFromAst(datatype: AstNode): Base {
        val tokens = datatype.tokens
        val first = datatype.getFirstChild().type
        val anchored = first == PlSqlGrammar.ANCHORED_DATATYPE
        val names = tokens.takeWhile { it.value != "%" }
            .let { list -> if (anchored || first == PlSqlGrammar.CUSTOM_DATATYPE) list else list.take(1) }.filter { it.value != "." }
        val kind = if (anchored) "ANCHORED:${tokens.last().value.uppercase()}" else "NAMED"
        return Base(kind, names.joinToString(".") { it.originalValue }, range(datatype))
    }

    private fun describe(ref: TypeRef) = Base(
        when (ref) {
            is NamedTypeRef -> "NAMED"
            is AnchoredTypeRef -> "ANCHORED:${ref.anchor}"
            is RefTypeRef -> "REF"
        },
        ref.name.segments.joinToString(".") { it.originalSpelling }, ref.sourceRange
    )

    private data class Expected(val base: Base, val declaration: SourceRange)

    private fun astSubtypes(source: String): Map<String, Expected> = parse(source).getDescendants(PlSqlGrammar.CREATE_PACKAGE).flatMap { pkg ->
        pkg.getFirstChild(PlSqlGrammar.DECLARE_SECTION).getChildren(PlSqlGrammar.CUSTOM_SUBTYPE).map {
            it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue to Expected(baseFromAst(it.getFirstChild(PlSqlGrammar.DATATYPE)), range(it))
        }
    }.toMap()

    private fun assertAgreesWithAst(source: String, count: Int) {
        val expected = astSubtypes(source)
        assertThat(expected).hasSize(count)
        val facts = subtypes(source)
        assertThat(facts.map { it.name.originalSpelling }).containsExactlyElementsOf(expected.keys)
        facts.forEach {
            val want = expected.getValue(it.name.originalSpelling)
            assertThat(describe(it.baseType)).describedAs(it.name.originalSpelling).isEqualTo(want.base)
            assertThat(it.sourceRange).describedAs(it.name.originalSpelling).isEqualTo(want.declaration)
            assertThat(it.owner).isEqualTo(QualifiedName(OracleIdentifier.fromSource("p")))
        }
    }

    @Test
    fun baseDatatypeEndsBeforeNullabilityAndRangeLikeTheParser() {
        assertAgreesWithAst(
            spec(
                "SUBTYPE a IS NUMBER;",
                "SUBTYPE b IS NUMBER NOT NULL;",
                "SUBTYPE c IS VARCHAR2(250) NOT NULL;",
                "SUBTYPE d IS RAW(32) NOT NULL;",
                "SUBTYPE e IS NUMBER NULL;",
                "SUBTYPE f IS PLS_INTEGER RANGE 1 .. 100;",
                "SUBTYPE g IS NUMBER NOT NULL RANGE 1 .. 100;",
                "SUBTYPE h IS source_type;",
                "SUBTYPE i IS source_type%TYPE;",
                "SUBTYPE j IS source_table%ROWTYPE;",
                "SUBTYPE k IS NUMBER(10,2) NOT NULL;",
                "SUBTYPE l IS VARCHAR2(30 CHAR) NULL;",
                "SUBTYPE m IS src.tab.col%TYPE NOT NULL;",
                "SUBTYPE n IS q.t NOT NULL;",
                "g1 NUMBER;",
                "PROCEDURE ok;"
            ),
            14
        )
    }

    @Test
    fun quotedKeywordLookalikesAreNamesNotClauses() {
        assertAgreesWithAst(
            spec(
                "SUBTYPE \"NOT\" IS \"NULL\";",
                "SUBTYPE \"NULL\" IS \"RANGE\" NOT NULL;",
                "SUBTYPE \"RANGE\" IS \"NOT\" NULL;"
            ),
            3
        )
    }

    @Test
    fun subtypeChainsKeepTheirNamesAndIdentities() {
        val source = spec("SUBTYPE money IS NUMBER NOT NULL;", "SUBTYPE amount IS money NOT NULL;", "SUBTYPE total IS amount;")
        assertAgreesWithAst(source, 3)
        val facts = subtypes(source)
        assertThat(facts.map { it.baseType.name.last.originalSpelling }).containsExactly("NUMBER", "money", "amount")
        assertThat(facts.map { it.baseType.structuralKey() }).isEqualTo(facts.map { "named:" + it.baseType.name.lookupKey() })
        assertThat(facts.map { it.kind }.toSet()).containsExactly(ProjectDeclarationKind.PACKAGE_SUBTYPE)
    }

    @Test
    fun fullDeclarationRangeAndFollowingMembersAreUnchanged() {
        val facts = extractor.extract(fileId, spec("SUBTYPE b IS NUMBER NOT NULL RANGE 1 .. 9;", "g NUMBER;", "PROCEDURE ok;"))
        val subtype = facts.filterIsInstance<PackageSubtypeDeclaration>().single()
        assertThat(subtype.sourceRange).isEqualTo(SourceRange(fileId, 2, 2, 2, 44))
        assertThat(subtype.baseType.sourceRange).isEqualTo(SourceRange(fileId, 2, 15, 2, 21))
        assertThat(facts.filterIsInstance<PackageValueDeclaration>().map { it.name.originalSpelling }).containsExactly("g")
        assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().map { it.name.originalSpelling }).containsExactly("ok")
    }

    @OptIn(ZpaExperimentalApi::class)
    @Test
    fun theCorrectedRangeIsVisibleThroughTheExistingPublicView() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("s.sql", spec("SUBTYPE b IS VARCHAR2(250) NOT NULL;"))), false)
        val view = snapshot.findDeclarations(ProjectQualifiedName.of("p", "b")).declarations.single() as ProjectSubtypeView
        assertThat(view.baseType.name).isEqualTo(ProjectQualifiedName.of("VARCHAR2"))
        assertThat(view.baseType.sourceRange.startColumn).isEqualTo(15)
        assertThat(view.baseType.sourceRange.endColumn).isEqualTo(28)
    }
}
