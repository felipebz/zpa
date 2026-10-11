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
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * The package specification path is bounded and token-typed. Valid syntax is checked
 * against the real parser; malformed input (no valid AST) is checked for conservative failure.
 */
class PackageSpecificationBoundaryTest {
    private val fileId = FileId("fixture/package.sql")
    private val extractor = ProjectDeclarationExtractor()
    private val good = "CREATE PACKAGE good AS PROCEDURE r; END good;"

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun packages(source: String) = extract(source).filterIsInstance<PackageDeclaration>()
    private fun isValue(fact: ProjectDeclaration) = fact is PackageValueDeclaration
    private fun members(source: String) = extract(source).filter { it !is PackageDeclaration && !isValue(it) }
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)

    private fun AstNode.range() = SourceRange(fileId, token.line, token.column, lastToken.endLine, lastToken.endColumn)

    private fun simpleName(fact: ProjectDeclaration) = fact.qualifiedName().last.originalSpelling

    /**
     * Valid syntax: container range and every type/subtype/subprogram member agree with the parser.
     * Value declarations are verified against the parser by the package value extraction tests.
     */
    private fun assertAgreesWithAst(source: String) {
        val root = parse(source) // throws if the parser rejects the source: the oracle must accept it
        val astPackages = root.getDescendants(PlSqlGrammar.CREATE_PACKAGE)
        val facts = extract(source)
        assertThat(facts.filterIsInstance<PackageDeclaration>()).describedAs(source).hasSameSizeAs(astPackages)
        facts.filterIsInstance<PackageDeclaration>().zip(astPackages).forEach { (fact, node) ->
            assertThat(fact.sourceRange).describedAs(source).isEqualTo(node.range())
        }
        val expected = astPackages.flatMap { pkg ->
            pkg.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)?.children.orEmpty().mapNotNull { child ->
                val kind = when (child.type) {
                    PlSqlGrammar.PROCEDURE_DECLARATION -> ProjectDeclarationKind.PACKAGE_PROCEDURE
                    PlSqlGrammar.FUNCTION_DECLARATION -> ProjectDeclarationKind.PACKAGE_FUNCTION
                    PlSqlGrammar.RECORD_DECLARATION, PlSqlGrammar.TABLE_OF_DECLARATION, PlSqlGrammar.VARRAY_DECLARATION,
                    PlSqlGrammar.REF_CURSOR_DECLARATION -> ProjectDeclarationKind.PACKAGE_TYPE
                    PlSqlGrammar.CUSTOM_SUBTYPE -> ProjectDeclarationKind.PACKAGE_SUBTYPE
                    else -> return@mapNotNull null
                }
                Triple(kind, child.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue, child.range())
            }
        }
        val actual = facts.filter { it !is PackageDeclaration && !isValue(it) }.map { Triple(it.kind, it.qualifiedName().last.originalSpelling, it.sourceRange) }
        assertThat(actual).describedAs(source).isEqualTo(expected)
    }

    @Test
    fun caseExpressionsInInitializersDoNotEndThePackageOrHideLaterMembers() {
        listOf(
            "CREATE PACKAGE p AS g NUMBER := CASE WHEN 1=1 THEN 1 ELSE 2 END; PROCEDURE q; h NUMBER; END p;",
            "CREATE PACKAGE p AS\n  g NUMBER;\n  PROCEDURE q(a NUMBER DEFAULT CASE WHEN 1=1 THEN 1 END);\n  h NUMBER := CASE WHEN 1=1 THEN 1 END;\n  PROCEDURE after_case;\n  FUNCTION f RETURN NUMBER;\nEND p;",
            "CREATE PACKAGE p AS g NUMBER := CASE g WHEN 1 THEN CASE WHEN 1=1 THEN 1 END ELSE 2 END; TYPE t IS TABLE OF NUMBER; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS CURSOR c IS SELECT CASE WHEN 1=1 THEN 1 END x FROM dual; PROCEDURE q; END;"
        ).forEach(::assertAgreesWithAst)
        val facts = members("CREATE PACKAGE p AS g NUMBER := CASE WHEN 1=1 THEN 1 ELSE 2 END; PROCEDURE q; h NUMBER; END p;")
        assertThat(facts.map { simpleName(it) }).containsExactly("q")
    }

    @Test
    fun caseAndFunctionAsIdentifiersAreHandledStructurally() {
        // The parser accepts all of these: CASE and FUNCTION are non-reserved words.
        listOf(
            "CREATE PACKAGE p AS\n  case NUMBER;\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  g NUMBER := function(1);\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  function NUMBER;\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  g NUMBER := t.case + 1;\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  g NUMBER := case(1);\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  g NUMBER := CASE WHEN 1=1 THEN case ELSE 2 END;\n  PROCEDURE q;\nEND p;",
            "CREATE PACKAGE p AS\n  g NUMBER := function(1);\n  function NUMBER;\n  FUNCTION f RETURN NUMBER;\n  FUNCTION h(a NUMBER) RETURN NUMBER;\n  PROCEDURE q;\nEND p;"
        ).forEach(::assertAgreesWithAst)
        // Both variables named like keywords AND the real function and procedure are all visible/consistent.
        assertThat(members("CREATE PACKAGE p AS\n  function NUMBER;\n  FUNCTION f RETURN NUMBER;\n  PROCEDURE q;\nEND p;").map { simpleName(it) })
            .containsExactly("f", "q")
    }

    @Test
    fun quotedKeywordLookalikesCannotAlterStructure() {
        listOf(
            "CREATE PACKAGE p AS \"END\" NUMBER; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS g NUMBER := \"END\"; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS \"IS\" NUMBER; \"AS\" NUMBER := 1; \"(\" NUMBER; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS \"PROCEDURE\" NUMBER; \"CASE\" NUMBER; PROCEDURE q; END p;"
        ).forEach(::assertAgreesWithAst)
        assertThat(members("CREATE PACKAGE p AS \"END\" NUMBER; PROCEDURE q; END p;").map { simpleName(it) }).containsExactly("q")
    }

    @Test
    fun variablesNamedLikeTypeDeclarationKeywordsAreValuesNotTypes() {
        listOf(
            "CREATE PACKAGE p AS type NUMBER; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS \"TYPE\" NUMBER; subtype NUMBER; \"SUBTYPE\" NUMBER; PROCEDURE q; END p;",
            "CREATE PACKAGE p AS type NUMBER; TYPE t IS RECORD (a NUMBER); SUBTYPE s IS NUMBER; PROCEDURE q; END p;"
        ).forEach(::assertAgreesWithAst)
        assertThat(extract("CREATE PACKAGE p AS type NUMBER; \"TYPE\" NUMBER; PROCEDURE q; END p;").map { it.kind })
            .containsExactly(
                ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_VARIABLE,
                ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE
            )
    }

    @Test
    fun packageNamedBodyInQuotesIsASpecification() {
        listOf(
            "CREATE PACKAGE \"BODY\" AS PROCEDURE q; END;",
            "CREATE OR REPLACE PACKAGE \"BODY\" AS FUNCTION f RETURN NUMBER; END \"BODY\";"
        ).forEach(::assertAgreesWithAst)
        val facts = extract("CREATE PACKAGE \"BODY\" AS PROCEDURE q; END;")
        assertThat(facts.filterIsInstance<PackageDeclaration>().single().role).isEqualTo(DeclarationRole.SPECIFICATION)
        assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().single().owner.toString()).isEqualTo("\"BODY\"")
        // A real BODY keyword is still a body.
        assertThat(extract("CREATE PACKAGE BODY p AS PROCEDURE q IS BEGIN NULL; END; END p;").single().role).isEqualTo(DeclarationRole.BODY)
    }

    @Test
    fun ordinarySpecificationsKeepTheirFactsAndRanges() {
        listOf(
            "CREATE PACKAGE p AS END p;",
            "CREATE PACKAGE p AS END;",
            "CREATE OR REPLACE EDITIONABLE PACKAGE app.p AUTHID DEFINER ACCESSIBLE BY (PACKAGE s.q) AS PROCEDURE q; END p;",
            "CREATE PACKAGE IF NOT EXISTS p AS PROCEDURE q; END p;",
            "CREATE PACKAGE p AS g NUMBER; c CONSTANT VARCHAR2(10) := 'x'; e EXCEPTION; PRAGMA EXCEPTION_INIT(e, -20001); END p;",
            "CREATE PACKAGE p AS\n  TYPE r IS RECORD (a NUMBER := 1, b VARCHAR2(5));\n  TYPE t IS TABLE OF r;\n  TYPE rc IS REF CURSOR;\n  SUBTYPE s IS NUMBER;\n  CURSOR cu IS SELECT 1 FROM dual;\n  PROCEDURE q(a IN NUMBER, b OUT NOCOPY CLOB);\n  FUNCTION f(a NUMBER DEFAULT 1) RETURN NUMBER DETERMINISTIC;\nEND p;",
            "CREATE PACKAGE p AS PROCEDURE q; END p;\n/\nCREATE PACKAGE q AS FUNCTION f RETURN NUMBER; END q;\n/\n",
            "CREATE PACKAGE p AS g NUMBER := /* ; END; */ 1; -- END;\n h NUMBER := 'a;END;'; PROCEDURE q; END p;"
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun wrappedSpecificationsAreSkippedWithoutConsumingFollowingUnits() {
        val source = "CREATE PACKAGE w wrapped\na000000\nabcd AS g NUMBER; END w;\n/\nCREATE PACKAGE q AS h NUMBER; PROCEDURE r; END q;\n/\n"
        val facts = extract(source)
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("q", "q.h", "q.r")
        assertThat(facts.first().sourceRange.startLine).isEqualTo(5)
        // No name-only fact for the wrapped package, and no members attributed to it.
        assertThat(packages(source).map { it.name.toString() }).doesNotContain("w")
        // Without a terminating `/` the lexer makes the rest of the file the opaque payload: nothing is extracted.
        assertThat(extract("CREATE PACKAGE w WRAPPED\nCREATE PACKAGE q AS PROCEDURE r; END q;\n")).isEmpty()
        // A WRAPPED keyword with no payload token at all is not a valid unit; the next unit survives.
        val noPayload = "CREATE PACKAGE w WRAPPED;\nCREATE PACKAGE q AS PROCEDURE r; END q;\n"
        assertThat(extract(noPayload).map { it.qualifiedName().toString() }).containsExactly("q", "q.r")
    }

    @Test
    fun malformedOrUnterminatedSpecificationsYieldNoFactAndNextUnitSurvives() {
        listOf(
            "CREATE PACKAGE bad\n$good",                                      // no IS/AS before the next unit
            "CREATE PACKAGE bad IS\n$good",                                   // header but no END before the next unit
            "CREATE PACKAGE bad AS PROCEDURE q;\n$good",
            "CREATE PACKAGE bad AS g NUMBER := 1;\n$good",
            "CREATE PACKAGE bad AS PROCEDURE q; END bad\n$good",              // END without `;`
            "CREATE PACKAGE bad AS PROCEDURE q; END p q;\n$good",              // END that is not a terminator
            "CREATE PACKAGE bad AS g NUMBER := (1;\n$good",                   // unbalanced
            "CREATE PACKAGE bad AS g NUMBER := f(1];\n$good",                 // crossed delimiters
            "CREATE PACKAGE bad AS PROCEDURE q;\n/\n$good",                   // lone slash bounds the unit
            "CREATE PACKAGE a.b.bad AS PROCEDURE q; END;\n$good",             // three-segment name
            "CREATE OR REPLACE PACKAGE IF NOT EXISTS bad AS PROCEDURE q; END;\n$good",
            "\"CREATE\" PACKAGE bad AS PROCEDURE q; END bad;\n$good",         // quoted CREATE
            "CREATE \"PACKAGE\" bad AS PROCEDURE q; END bad;\n$good"
        ).forEach { source ->
            val facts = extract(source)
            assertThat(facts.map { it.qualifiedName().toString() }).describedAs(source).containsExactly("good", "good.r")
            assertThat(facts.first().sourceRange.startColumn).isEqualTo(0)
            assertThat(facts.first().sourceRange.startLine).isEqualTo(source.lines().indexOfFirst { it == good } + 1)
        }
        // No terminating END: no EOF-wide fact either.
        assertThat(extract("CREATE PACKAGE p AS g NUMBER := 1;")).isEmpty()
        assertThat(extract("CREATE PACKAGE p AS PROCEDURE q;")).isEmpty()
    }

    @Test
    fun missingSemicolonBeforeAProcedureDropsOnlyTheBrokenDeclaration() {
        val facts = extract("CREATE PACKAGE p AS g NUMBER := 1 PROCEDURE q; PROCEDURE r; END p;")
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("p", "p.q", "p.r")
    }

    @Test
    fun packageBodiesStandaloneUnitsAndOtherFactsAreUnchanged() {
        val source = """
            CREATE PACKAGE p AS PROCEDURE q; END p;
            /
            CREATE PACKAGE BODY p AS
              priv NUMBER := CASE WHEN 1=1 THEN 1 END;
              PROCEDURE q IS loc NUMBER; BEGIN NULL; END;
            END p;
            /
            CREATE FUNCTION f(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;
            /
            CREATE PROCEDURE sp IS BEGIN NULL; END;
            CREATE TYPE t AS OBJECT (id NUMBER);
            CREATE SEQUENCE s;
        """.trimIndent()
        assertThat(extract(source).map { it.kind to it.qualifiedName().toString() }).containsExactly(
            ProjectDeclarationKind.PACKAGE to "p",
            ProjectDeclarationKind.PACKAGE_PROCEDURE to "p.q",
            ProjectDeclarationKind.PACKAGE_PROCEDURE to "p.q",
            ProjectDeclarationKind.STANDALONE_FUNCTION to "f",
            ProjectDeclarationKind.STANDALONE_PROCEDURE to "sp",
            ProjectDeclarationKind.STANDALONE_TYPE to "t",
            ProjectDeclarationKind.SEQUENCE to "s"
        )
        assertThat(extract(source).filter { it.role == DeclarationRole.BODY }).hasSize(1)
    }

    @Test
    fun createLessPackageUnitsRemainOutOfScope() {
        assertThat(extract("PACKAGE p IS g NUMBER; PROCEDURE q; END p;")).isEmpty()
    }
}
