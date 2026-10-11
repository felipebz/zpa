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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Package specification walking, with the extractor built from the shared per-extraction components. */
class PackageSpecificationExtractorTest {
    private val fileId = FileId("spec.sql")

    private class Fixture(text: String, fileId: FileId) {
        val stream = TokenStream(fileId, PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        private val shape = DatatypeShape(stream)
        private val references = TypeReferences(stream)
        private val members = PackageMemberParser(stream, references)
        val extractor = PackageSpecificationExtractor(
            stream, UnitBounds(stream), PackageValueRecognizer(stream, shape, references, InitializerShape(stream)), members
        )
    }

    /** `CREATE PACKAGE p ...`: the name `p` is the third token, so it is at index 2. */
    private fun spec(text: String, nameIndex: Int = 2): Pair<ParsedUnit?, Fixture> =
        Fixture(text, fileId).let { it.extractor.parsePackageSpecification(0, nameIndex) to it }

    private fun facts(text: String) = ProjectDeclarationExtractor().extract(fileId, text)

    @Test
    fun headerOnlyPackagesAndTerminatorForms() {
        listOf("CREATE PACKAGE p AS END p;", "CREATE PACKAGE p IS END;", "CREATE PACKAGE p AS END \"p\";").forEach { text ->
            val (unit, fixture) = spec(text)
            val declaration = unit!!.declarations.single() as PackageDeclaration
            assertThat(declaration.role).isEqualTo(DeclarationRole.SPECIFICATION)
            assertThat(declaration.name.toString()).isEqualTo("p")
            assertThat(declaration.sourceRange).describedAs(text).isEqualTo(SourceRange(fileId, 1, 0, 1, text.length))
            assertThat(unit.nextIndex).isEqualTo(fixture.stream.tokens.size - 1)
        }
        listOf(
            "CREATE PACKAGE p AS", "CREATE PACKAGE p AS PROCEDURE q;", "CREATE PACKAGE p AS END", "CREATE PACKAGE p AS END p q;",
            "CREATE PACKAGE p END;", "CREATE PACKAGE p;", "CREATE PACKAGE AS END;"
        ).forEach { assertThat(spec(it).first).describedAs(it).isNull() }
        assertThat(spec("CREATE PACKAGE a.b.c AS END;").first).describedAs("three segments").isNull()
    }

    @Test
    fun membersAreEmittedInSourceOrderAfterTheContainer() {
        val text = """
            CREATE PACKAGE p AS
              TYPE r IS RECORD (a NUMBER := 1, b VARCHAR2(5));
              SUBTYPE s IS NUMBER;
              g NUMBER := CASE WHEN 1 = 1 THEN 1 ELSE 2 END;
              c CONSTANT VARCHAR2(10) := 'x';
              e EXCEPTION;
              PRAGMA SERIALLY_REUSABLE;
              CURSOR cu IS SELECT 1 FROM dual;
              PROCEDURE q(a IN NUMBER, b OUT NOCOPY VARCHAR2);
              FUNCTION f(a NUMBER) RETURN NUMBER DETERMINISTIC;
              pragma NUMBER;
            END p;
        """.trimIndent()
        val (unit, fixture) = spec(text)
        val declarations = unit!!.declarations
        assertThat(declarations.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_TYPE, ProjectDeclarationKind.PACKAGE_SUBTYPE,
            ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_CONSTANT,
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.PACKAGE_VARIABLE
        )
        assertThat(declarations.map { it.role }).containsOnly(DeclarationRole.SPECIFICATION)
        assertThat(declarations.map { it.fileId }).containsOnly(fileId)
        assertThat((declarations[1] as PackageTypeDeclaration).recordFields.map { it.ordinal }).containsExactly(0, 1)
        assertThat((declarations[5] as PackageProcedureDeclaration).parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat((declarations[6] as PackageFunctionDeclaration).deterministic).isTrue()
        assertThat((declarations[3] as PackageValueDeclaration).name.originalSpelling).isEqualTo("g")
        assertThat((declarations[7] as PackageValueDeclaration).name.originalSpelling).isEqualTo("pragma")
        assertThat(declarations.drop(1).map { it.sourceRange.startLine }).containsExactly(2, 3, 4, 5, 9, 10, 11)
        assertThat(unit.nextIndex).isEqualTo(fixture.stream.tokens.size - 1)
    }

    @Test
    fun valuesAreTriedBeforeMembersAndMalformedDeclarationsAreDroppedWithoutLosingNeighbours() {
        // `cursor NUMBER;` is a variable for the grammar, hence a value, not a cursor.
        val values = facts("CREATE PACKAGE p AS cursor NUMBER; function NUMBER; type NUMBER; PROCEDURE q; END p;")
        assertThat(values.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_VARIABLE,
            ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE
        )
        val malformed = facts("CREATE PACKAGE p AS g NUMBER b;\n  PROCEDURE q;\n  h NUMBER := CASE WHEN x THEN 1;\n  FUNCTION f RETURN NUMBER;\nEND p;")
        assertThat(malformed.map { it.qualifiedName().toString() }).containsExactly("p", "p.q", "p.f")
        // A missing `;` before a reserved PROCEDURE drops only the broken declaration; the procedure is still visited.
        val missing = facts("CREATE PACKAGE p AS a NUMBER := 1\n  PROCEDURE q;\n  b NUMBER;\nEND p;")
        assertThat(missing.map { it.qualifiedName().toString() }).containsExactly("p", "p.q", "p.b")
        // Unbalanced delimiters leave no strict boundary: nothing from this unit, and the next unit survives.
        val unbalanced = facts("CREATE PACKAGE bad AS g NUMBER := (1; h NUMBER; END bad;\nCREATE PACKAGE good AS PROCEDURE r; END good;")
        assertThat(unbalanced.map { it.qualifiedName().toString() }).containsExactly("good", "good.r")
    }

    @Test
    fun wrappedSpecificationsAreSkippedWithAnEmptyResultAndTheNextUnitSurvives() {
        val text = "CREATE PACKAGE w wrapped\na000000\nabcd AS g NUMBER; END w;\n/\nCREATE PACKAGE q AS h NUMBER; END q;\n"
        val (unit, fixture) = spec(text)
        assertThat(unit!!.declarations).isEmpty()
        assertThat(fixture.stream.tokenTypeAt(unit.nextIndex - 1)).isEqualTo(com.felipebz.zpa.api.PlSqlTokenType.WRAPPED_SOURCE)
        assertThat(facts(text).map { it.qualifiedName().toString() }).containsExactly("q", "q.h")
        assertThat(spec("CREATE PACKAGE w WRAPPED;").first).describedAs("no payload token").isNull()
    }

    @Test
    fun headerFormsAndUnitLimitsThroughTheFacade() {
        val facts = facts(
            "CREATE OR REPLACE EDITIONABLE PACKAGE app.p AUTHID DEFINER ACCESSIBLE BY (PACKAGE s.q) AS PROCEDURE x; END p;\n" +
                "CREATE PACKAGE IF NOT EXISTS \"Q\" AS FUNCTION y RETURN NUMBER; END;\n/\nCREATE PACKAGE unfinished AS PROCEDURE z;\n/\n" +
                "CREATE PACKAGE last AS g NUMBER; END last;"
        )
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("app.p", "app.p.x", "\"Q\"", "\"Q\".y", "last", "last.g")
        assertThat(facts.filterIsInstance<PackageDeclaration>().map { it.role }).containsOnly(DeclarationRole.SPECIFICATION)
    }
}
