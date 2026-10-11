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

/** Strict standalone procedures/functions, with the extractor built from the shared per-extraction components. */
class StandaloneCallableExtractorTest {
    private val fileId = FileId("standalone.sql")

    private class Fixture(text: String, fileId: FileId) {
        val stream = TokenStream(fileId, PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        private val shape = DatatypeShape(stream)
        val extractor = StandaloneCallableExtractor(
            stream, UnitBounds(stream), shape, TypeReferences(stream), CallableProperties(stream),
            CallSpecificationScanner(stream, shape), BodyBoundaryScanner(stream)
        )
    }

    private fun procedure(text: String, at: Int = 0): ParsedUnit? = Fixture(text, fileId).extractor.parseStandaloneCallable(at, function = false)
    private fun function(text: String, at: Int = 0): ParsedUnit? = Fixture(text, fileId).extractor.parseStandaloneCallable(at, function = true)

    @Test
    fun procedureParametersKeepModesNocopyDefaultsOrdinalsAndRanges() {
        val text = "CREATE OR REPLACE PROCEDURE app.p(a NUMBER, b OUT NOCOPY VARCHAR2, c IN OUT t.x%TYPE, d NUMBER DEFAULT 1, e NUMBER := f(1, 2), \"F\" REF o) IS BEGIN NULL; END;"
        val unit = procedure(text)!!
        val declaration = unit.declarations.single() as StandaloneProcedureDeclaration
        assertThat(declaration.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_PROCEDURE)
        assertThat(declaration.role).isEqualTo(DeclarationRole.STANDALONE)
        assertThat(declaration.name.toString()).isEqualTo("app.p")
        assertThat(declaration.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, text.length))
        assertThat(unit.nextIndex).isEqualTo(Fixture(text, fileId).stream.tokens.size - 1)
        assertThat(declaration.parameters.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5, 6)
        assertThat(declaration.parameters.map { it.mode }).containsExactly(
            ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN_OUT, ParameterMode.IN, ParameterMode.IN, ParameterMode.IN
        )
        assertThat(declaration.parameters.map { it.nocopy }).containsExactly(false, true, false, false, false, false)
        assertThat(declaration.parameters.map { it.defaultPresent }).containsExactly(false, false, false, true, true, false)
        assertThat(declaration.parameters[2].typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(declaration.parameters[5].typeRef).isInstanceOf(RefTypeRef::class.java)
        assertThat(declaration.parameters[5].name.originalSpelling).isEqualTo("\"F\"")
        assertThat(declaration.parameters[0].sourceRange).isEqualTo(SourceRange(fileId, 1, 34, 1, 42))
    }

    @Test
    fun absentParameterListsAreValidButEmptyOrMalformedListsRejectTheWholeUnit() {
        assertThat((procedure("CREATE PROCEDURE p IS BEGIN NULL; END;")!!.declarations.single() as StandaloneProcedureDeclaration).parameters).isEmpty()
        listOf(
            "CREATE PROCEDURE p() IS BEGIN NULL; END;", "CREATE PROCEDURE p(a NUMBER b) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a NUMBER,) IS BEGIN NULL; END;", "CREATE PROCEDURE p(a NUMBER IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a OUT NUMBER DEFAULT 1) IS BEGIN NULL; END;", "CREATE PROCEDURE p(a NOCOPY NUMBER) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a [NUMBER) IS BEGIN NULL; END;", "CREATE PROCEDURE p(a NUMBER DEFAULT) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a f(1]) IS BEGIN NULL; END;"
        ).forEach { assertThat(procedure(it)).describedAs(it).isNull() }
    }

    @Test
    fun functionsCarryReturnTypeDeterministicFlagAndRanges() {
        val text = "CREATE FUNCTION s.f(a NUMBER) RETURN VARCHAR2(10) DETERMINISTIC RESULT_CACHE IS BEGIN RETURN 'x'; END;"
        val unit = function(text)!!
        val f = unit.declarations.single() as StandaloneFunctionDeclaration
        assertThat(f.deterministic).isTrue()
        assertThat((f.returnType as NamedTypeRef).name.toString()).isEqualTo("VARCHAR2")
        assertThat(f.returnType.sourceRange).isEqualTo(SourceRange(fileId, 1, 37, 1, 49))
        assertThat(f.parameters).hasSize(1)
        assertThat((function("CREATE FUNCTION g RETURN t.c%TYPE IS BEGIN RETURN 1; END;")!!.declarations.single() as StandaloneFunctionDeclaration).returnType)
            .isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(function("CREATE FUNCTION g RETURN NUMBER;")).describedAs("body-less declaration").isNotNull()
        assertThat(function("CREATE FUNCTION g IS BEGIN RETURN 1; END;")).describedAs("no RETURN").isNull()
        assertThat(function("CREATE FUNCTION g RETURN IS BEGIN RETURN 1; END;")).describedAs("empty return type").isNull()
        assertThat(function("CREATE FUNCTION g RETURN NUMBER x IS BEGIN RETURN 1; END;")).describedAs("extra identifier").isNull()
    }

    @Test
    fun terminalPropertyFormsAreFunctionOnlyAndEndAtTheirSemicolon() {
        val text = "CREATE FUNCTION f RETURN NUMBER PIPELINED USING impl;"
        val unit = function(text)!!
        assertThat((unit.declarations.single() as StandaloneFunctionDeclaration).deterministic).isFalse()
        assertThat(unit.nextIndex).isEqualTo(Fixture(text, fileId).stream.tokens.size - 1)
        assertThat(function("CREATE FUNCTION f RETURN NUMBER DETERMINISTIC AGGREGATE USING impl;")).isNotNull()
        assertThat(procedure("CREATE PROCEDURE p PIPELINED USING impl;")).isNull()
        assertThat(procedure("CREATE PROCEDURE p AUTHID DEFINER IS BEGIN NULL; END;")).isNotNull()
    }

    @Test
    fun callSpecificationsAndQuotedLookalikes() {
        listOf(
            "CREATE PROCEDURE p AS LANGUAGE JAVA NAME 'A.b()';", "CREATE PROCEDURE p AS LANGUAGE C LIBRARY lib;",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT);", "CREATE PROCEDURE p AS MLE MODULE m SIGNATURE 'f()';",
            "CREATE PROCEDURE p AS MLE LANGUAGE JAVASCRIPT {{ x }};"
        ).forEach { assertThat(procedure(it)).describedAs(it).isNotNull() }
        assertThat(function("CREATE FUNCTION f RETURN NUMBER AS LANGUAGE JAVA NAME 'A.b()';")).isNotNull()
        listOf(
            "CREATE PROCEDURE p AS LANGUAGE \"C\" LIBRARY lib;", "CREATE PROCEDURE p AS \"LANGUAGE\" C LIBRARY lib;",
            "CREATE PROCEDURE p AS LANGUAGE JAVA NAME x;", "CREATE PROCEDURE p AS LANGUAGE JAVA;",
            "CREATE PROCEDURE p AS EXTERNAL;", "CREATE PROCEDURE p AS MLE MODULE m SIGNATURE x;"
        ).forEach { assertThat(procedure(it)).describedAs(it).isNull() }
    }

    @Test
    fun unitLimitsAndAdjacentUnitsAreRespected() {
        val text = "CREATE PROCEDURE a IS BEGIN NULL; END;\n/\nCREATE PROCEDURE b IS BEGIN NULL; END;"
        val fixture = Fixture(text, fileId)
        val first = fixture.extractor.parseStandaloneCallable(0, function = false)!!
        assertThat(fixture.stream.isPunctuator(first.nextIndex, com.felipebz.zpa.api.PlSqlPunctuator.DIVISION)).isTrue()
        val secondStart = fixture.stream.tokens.indices.last { fixture.stream.isKeyword(it, "CREATE") }
        val second = fixture.extractor.parseStandaloneCallable(secondStart, function = false)!!
        assertThat((second.declarations.single() as StandaloneProcedureDeclaration).name.toString()).isEqualTo("b")
        assertThat(second.declarations.single().sourceRange).isEqualTo(SourceRange(fileId, 3, 0, 3, 38))
        // An implementation that runs into the next CREATE is not completed, and does not consume it.
        assertThat(procedure("CREATE PROCEDURE p IS BEGIN NULL;\nCREATE PROCEDURE q IS BEGIN NULL; END;")).isNull()
        assertThat(procedure("CREATE PROCEDURE p IS BEGIN NULL;")).isNull()
        assertThat(procedure("CREATE PROCEDURE p")).isNull()
        assertThat(procedure("CREATE PROCEDURE a.b.c IS BEGIN NULL; END;")).describedAs("three-segment name").isNull()
        assertThat(function("CREATE PROCEDURE p IS BEGIN NULL; END;")).describedAs("wrong kind for the requested form").isNull()
    }

    @Test
    fun theFacadeSkipsRejectedUnitsAndKeepsLaterOnes() {
        val facts = ProjectDeclarationExtractor().extract(
            fileId,
            "CREATE PROCEDURE bad(a NUMBER b) IS BEGIN NULL; END;\nCREATE FUNCTION good(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;\n" +
                "CREATE PROCEDURE \"Quoted\" AS LANGUAGE \"C\" LIBRARY lib;\nCREATE PROCEDURE last IS BEGIN NULL; END;"
        )
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("good", "last")
        assertThat(facts.map { it.kind }).containsExactly(ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.STANDALONE_PROCEDURE)
    }
}
