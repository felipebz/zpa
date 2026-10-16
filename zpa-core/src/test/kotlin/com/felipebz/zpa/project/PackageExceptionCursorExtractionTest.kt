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
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.MemberNameStatus.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Strict extraction of package specification exceptions and cursors. Valid shapes are compared with the real parser
 * (error recovery disabled); refused shapes keep the unretained name and coverage gaps they had before they could be
 * retained.
 */
class PackageExceptionCursorExtractionTest {
    private val fileId = FileId("fixture/members.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
    private fun spec(vararg declarations: String) = "CREATE PACKAGE p AS\n${declarations.joinToString("\n") { "  $it" }}\nEND p;"
    private fun coverage(source: String) = extract(source).filterIsInstance<PackageDeclaration>().single().memberNames!!
    private fun AstNode.range() = SourceRange(fileId, token.line, token.column, lastToken.endLine, lastToken.endColumn)
    private fun id(s: String) = OracleIdentifier.fromSource(s)

    private fun exceptions(source: String) = extract(source).filterIsInstance<PackageExceptionDeclaration>()
    private fun cursors(source: String) = extract(source).filterIsInstance<PackageCursorDeclaration>()

    /** Each retained cursor or exception has the range and name of the parser's declaration node. */
    private fun assertAgreesWithAst(source: String) {
        val nodes = parse(source).getDescendants(PlSqlGrammar.CURSOR_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
        val facts = extract(source).filter { it is PackageExceptionDeclaration || it is PackageCursorDeclaration }
        assertThat(facts.map { it.qualifiedName().last.originalSpelling to it.sourceRange }).describedAs(source)
            .isEqualTo(nodes.map { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue to it.range() })
    }

    private fun type(ref: TypeRef?) = when (ref) {
        null -> null
        is AnchoredTypeRef -> "${ref.name}%${ref.anchor}"
        else -> ref.name.toString()
    }

    @Test
    fun exceptions() {
        val source = spec("app_error EXCEPTION;", "PRAGMA EXCEPTION_INIT(app_error, -20999);", "\"Mixed Case\" EXCEPTION;", "Upper_Lower exception;")
        val facts = exceptions(source)
        assertThat(facts.map { it.name.originalSpelling }).containsExactly("app_error", "\"Mixed Case\"", "Upper_Lower")
        assertThat(facts.map { it.qualifiedName() }).containsExactly(
            QualifiedName(listOf(id("p"), id("app_error"))), QualifiedName(listOf(id("p"), id("\"Mixed Case\""))),
            QualifiedName(listOf(id("p"), id("UPPER_LOWER")))
        )
        assertThat(facts.map { it.kind }).containsOnly(ProjectDeclarationKind.PACKAGE_EXCEPTION)
        assertThat(facts.map { it.role }).containsOnly(DeclarationRole.SPECIFICATION)
        assertThat(facts.first().sourceRange).isEqualTo(SourceRange(fileId, 2, 2, 2, 22))
        // The pragma stays a nameless declaration: no fact, no name, no gap.
        assertThat(coverage(source).unretained).isEmpty()
        assertThat(coverage(source).complete).isTrue()
        assertAgreesWithAst(source)
    }

    @Test
    fun cursorShapes() {
        val source = spec(
            "TYPE rec_t IS RECORD (id NUMBER);",
            "CURSOR c_all IS SELECT id FROM dual;",
            "CURSOR c_param (p_status NUMBER, p_flag IN VARCHAR2 DEFAULT 'x') IS SELECT 1 id FROM dual WHERE 1 = p_status;",
            "CURSOR c_pending (p_status NUMBER) RETURN send_queue%ROWTYPE;",
            "CURSOR c_record RETURN rec_t IS SELECT 1 id FROM dual;",
            "CURSOR \"Quoted\" RETURN other.rec_t;",
            "CURSOR Mixed_Case RETURN c_all%ROWTYPE;"
        )
        val facts = cursors(source)
        assertThat(facts.map { it.name.originalSpelling })
            .containsExactly("c_all", "c_param", "c_pending", "c_record", "\"Quoted\"", "Mixed_Case")
        assertThat(facts.map { type(it.returnType) })
            .containsExactly(null, null, "send_queue%ROWTYPE", "rec_t", "other.rec_t", "c_all%ROWTYPE")
        assertThat(facts.map { c -> c.parameters.map { "${it.ordinal}:${it.name}:${it.mode}:${it.defaultPresent}:${it.typeRef.name}" } })
            .containsExactly(
                emptyList(), listOf("1:p_status:IN:false:NUMBER", "2:p_flag:IN:true:VARCHAR2"), listOf("1:p_status:IN:false:NUMBER"),
                emptyList(), emptyList(), emptyList()
            )
        assertThat(facts.map { it.kind }).containsOnly(ProjectDeclarationKind.PACKAGE_CURSOR)
        assertThat(facts[4].qualifiedName()).isEqualTo(QualifiedName(listOf(id("p"), id("\"Quoted\""))))
        assertThat(facts[5].qualifiedName()).isEqualTo(QualifiedName(listOf(id("p"), id("MIXED_CASE"))))
        assertThat(coverage(source).unretained).isEmpty()
        assertThat(coverage(source).complete).isTrue()
        assertAgreesWithAst(source)
    }

    /** Names and gaps of refused shapes, which must stay exactly as they were before cursors could be retained. */
    private fun refused(declaration: String): Pair<List<String>, Set<MemberNameGap>> {
        val source = spec(declaration, "PROCEDURE q;")
        val facts = extract(source)
        assertThat(facts.filter { it is PackageCursorDeclaration || it is PackageExceptionDeclaration }).describedAs(declaration).isEmpty()
        val c = coverage(source)
        return c.unretained.map { "${it.kind} ${it.name.originalSpelling}" } to c.gaps
    }

    @Test
    fun refusedCursorShapesKeepTheirNames() {
        val cursor = listOf("CURSOR c") to emptySet<MemberNameGap>()
        assertThat(refused("CURSOR c (p OUT NUMBER) IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c (p IN OUT NUMBER) IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c (p NOCOPY NUMBER) IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c (p NUMBER,) IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c () IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c RETURN t%ROWTYPE%TYPE;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c RETURN 1 IS SELECT 1 FROM dual;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c RETURN t IS;")).isEqualTo(cursor)
        assertThat(refused("CURSOR c IS ;")).isEqualTo(cursor)
        // No RETURN and no query: not a cursor. As before, `cursor` is a non-reserved word, so this is a value named
        // CURSOR of type c, retained as a value fact.
        assertThat(refused("CURSOR c;")).isEqualTo(emptyList<String>() to emptySet<MemberNameGap>())
        assertThat(extract(spec("CURSOR c;")).filterIsInstance<PackageValueDeclaration>().map { it.name.lookupName }).containsExactly("CURSOR")
        // A dotted name is not a member name.
        assertThat(refused("CURSOR a.b IS SELECT 1 FROM dual;")).isEqualTo(emptyList<String>() to setOf(MemberNameGap.UNIDENTIFIED_DECLARATION))
    }

    @Test
    fun aMissingSemicolonRetainsNothing() {
        // The declaration runs into the next subprogram: it is not terminated and keeps only its name.
        val source = spec("CURSOR c IS SELECT 1 FROM dual", "PROCEDURE q;")
        assertThat(cursors(source)).isEmpty()
        assertThat(coverage(source).unretained.map { "${it.kind} ${it.name}" }).containsExactly("CURSOR c")
        assertThat(coverage(source).gaps).containsExactly(MemberNameGap.UNTERMINATED_DECLARATION)
        // Two exceptions merged by a missing `;`: no exception shape, the leading name is a value with a swallowed gap.
        val merged = spec("e1 EXCEPTION", "e2 EXCEPTION;")
        assertThat(exceptions(merged)).isEmpty()
        assertThat(coverage(merged).unretained.map { "${it.kind} ${it.name}" }).containsExactly("VALUE e1")
        assertThat(coverage(merged).gaps).containsExactly(MemberNameGap.SWALLOWED_DECLARATION)
    }

    @Test
    fun aRetainedCursorKeepsTheCoverageChecksOfItsDeclaration() {
        // A query ending in a function heading suggests a missing `;`: the cursor is retained and the gaps stay.
        val source = spec("CURSOR c IS SELECT 1 FROM dual FUNCTION f RETURN NUMBER;")
        assertThat(cursors(source)).hasSize(1)
        val c = coverage(source)
        assertThat(c.unretained).isEmpty()
        assertThat(c.gaps).containsExactly(MemberNameGap.SWALLOWED_DECLARATION)
        val callables = extract(source).filterIsInstance<PackageDeclaration>().single().callableCoverage
        assertThat(callables.gaps).containsExactly(CallableCoverageGap.SWALLOWED_CALLABLE_HEADER)
    }

    @Test
    fun bodyCursorsAndExceptionsProduceNoFacts() {
        val body = "CREATE PACKAGE BODY p AS\n  e EXCEPTION;\n  CURSOR c RETURN t IS SELECT 1 FROM dual;\n  PROCEDURE q IS BEGIN NULL; END;\nEND p;"
        assertThat(extract(body).filter { it is PackageCursorDeclaration || it is PackageExceptionDeclaration }).isEmpty()
    }

    @Test
    fun memberNameStatusOfRetainedExceptionsAndCursors() {
        fun status(source: String, member: String, holder: String): MemberNameStatus {
            val index = ProjectIndexPreparation(extractor).prepare(listOf(ProjectSource(FileId("spec.sql")) { source }), false).index
            val unit = index.findOccurrences(QualifiedName(id("p"))).single()
            val holderFact = index.findOccurrences(QualifiedName(listOf(id("p"), id(holder)))).single().declaration as PackageTypeDeclaration
            return PackageMemberNames.at(index, unit, id(member), holderFact.elementType!!.sourceRange).status
        }
        fun ccSpec(vararg lines: String) = "CREATE PACKAGE p AS\n  TYPE r_t IS RECORD (a NUMBER);\n${lines.joinToString("\n") { "  $it" }}\nEND p;"
        assertThat(status(ccSpec("CURSOR emp RETURN r_t;", "TYPE rows_t IS TABLE OF emp%ROWTYPE;"), "emp", "rows_t")).isEqualTo(DECLARED_BEFORE)
        assertThat(status(ccSpec("TYPE rows_t IS TABLE OF emp%ROWTYPE;", "CURSOR emp RETURN r_t;"), "emp", "rows_t")).isEqualTo(REJECTED)
        assertThat(status(ccSpec("TYPE rows_t IS TABLE OF emp%ROWTYPE;", "emp EXCEPTION;"), "emp", "rows_t")).isEqualTo(REJECTED)
        assertThat(status(ccSpec("emp EXCEPTION;", "TYPE rows_t IS TABLE OF emp%ROWTYPE;"), "emp", "rows_t")).isEqualTo(DECLARED_BEFORE)
        assertThat(status(ccSpec("\$IF \$\$f \$THEN CURSOR emp RETURN r_t; \$END", "TYPE rows_t IS TABLE OF emp%ROWTYPE;"), "emp", "rows_t"))
            .isEqualTo(UNKNOWN)
        assertThat(status(ccSpec("\$IF \$\$f \$THEN emp EXCEPTION; \$END", "TYPE rows_t IS TABLE OF emp%ROWTYPE;"), "emp", "rows_t"))
            .isEqualTo(UNKNOWN)
    }
}
