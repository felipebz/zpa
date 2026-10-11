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

/** Recognition of one package-specification value declaration, called on a hand-lexed `name ... ;` slice. */
class PackageValueRecognizerTest {
    private val fileId = FileId("values.sql")
    private val owner = QualifiedName(com.felipebz.zpa.project.OracleIdentifier.fromSource("p"))

    private fun recognize(text: String): Pair<ParsedDeclaration?, Int> {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text)
        val stream = TokenStream(fileId, tokens)
        val shape = DatatypeShape(stream)
        val references = TypeReferences(stream)
        val recognizer = PackageValueRecognizer(stream, shape, references, InitializerShape(stream))
        val semicolon = tokens.indexOfLast { it.value == ";" }
        return recognizer.parseSpecificationValue(0, semicolon, owner) to semicolon
    }

    private fun value(text: String): PackageValueDeclaration {
        val (parsed, semicolon) = recognize(text)
        val result = parsed ?: error("expected a value for: $text")
        assertThat(result.nextIndex).isEqualTo(semicolon + 1)
        return result.declaration as PackageValueDeclaration
    }

    @Test
    fun variablesAndConstantsCarryNameClassificationTypeAndRanges() {
        val g = value("g NUMBER;")
        assertThat(g.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(g.owner).isEqualTo(owner)
        assertThat(g.name.originalSpelling).isEqualTo("g")
        assertThat(g.typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat(g.typeRef.sourceRange).isEqualTo(SourceRange(fileId, 1, 2, 1, 8))
        assertThat(g.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 9))
        val c = value("\"My Const\" CONSTANT VARCHAR2(10 CHAR) NOT NULL := 'x';")
        assertThat(c.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(c.name.originalSpelling).isEqualTo("\"My Const\"")
        assertThat(c.typeRef.sourceRange).isEqualTo(SourceRange(fileId, 1, 20, 1, 37))
        assertThat(c.sourceRange.startColumn).isEqualTo(0)
    }

    @Test
    fun datatypeFormsAndNullabilityAndInitializers() {
        assertThat(value("a s.t;").typeRef.name.toString()).isEqualTo("s.t")
        assertThat(value("a t.c%TYPE := 1;").typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(value("a NUMBER(10, -2) NULL;").typeRef.sourceRange).isEqualTo(SourceRange(fileId, 1, 2, 1, 16))
        assertThat(value("a NUMBER(*, 0) NOT NULL DEFAULT 1;").typeRef.sourceRange.endColumn).isEqualTo(14)
        assertThat(value("a TIMESTAMP(3) WITH TIME ZONE := SYSTIMESTAMP;").typeRef.name.toString()).isEqualTo("TIMESTAMP")
        assertThat(value("a NUMBER := CASE WHEN 1 = 1 THEN 1 END;").sourceRange.endColumn).isEqualTo(39)
        assertThat(value("a NUMBER := case(1);")).isNotNull()
        assertThat(value("a NUMBER := function(1);")).isNotNull()
    }

    @Test
    fun pragmasAndExceptionsAreNotValuesButAVariableNamedPragmaIs() {
        listOf(
            "PRAGMA SERIALLY_REUSABLE;", "PRAGMA AUTONOMOUS_TRANSACTION;", "PRAGMA UDF;", "PRAGMA EXCEPTION_INIT(e, -20001);",
            "PRAGMA COVERAGE('NOT_FEATURE');", "e EXCEPTION;"
        ).forEach { assertThat(recognize(it).first).describedAs(it).isNull() }
        assertThat(value("pragma NUMBER;").name.originalSpelling).isEqualTo("pragma")
        assertThat(value("pragma t.x%TYPE := 1;").typeRef).isInstanceOf(AnchoredTypeRef::class.java)
    }

    @Test
    fun unsupportedOrMalformedDeclarationsYieldNull() {
        listOf(
            "g NUMBER b;", "g NUMBER :=;", "g NUMBER := CASE WHEN x THEN 1;", "g NUMBER(a NUMBER);", "g NUMBER NOT := 1;",
            "g NUMBER NOT NULL NOT NULL;", "g;", "g CONSTANT;", "g NUMBER DEFAULT;", "g NUMBER := (1;", "g SYS.X@link;"
        ).forEach { assertThat(recognize(it).first).describedAs(it).isNull() }
        assertThat(recognize("CREATE x;").first).isNull()
        assertThat(recognize("1 NUMBER;").first).isNull()
    }

    @Test
    fun initializerMemoizationDoesNotLeakBetweenDeclarationsOfOneSpecification() {
        val source = """
            CREATE PACKAGE p AS
              a NUMBER := CASE WHEN 1 = 1 THEN 1 ELSE 2 END;
              b NUMBER := CASE WHEN x THEN 1;
              c NUMBER := CASE WHEN 1 = 1 THEN CASE WHEN 2 = 2 THEN 1 END END;
              d NUMBER := case(1);
              e NUMBER := CASE WHEN 1 = 1 THEN case ELSE 2 END;
            END p;
        """.trimIndent()
        val together = ProjectDeclarationExtractor().extract(fileId, source).filterIsInstance<PackageValueDeclaration>()
        assertThat(together.map { it.name.originalSpelling }).containsExactly("a", "c", "d", "e")
        // Each admitted declaration alone gives the same answer: nothing is carried over from its neighbours.
        listOf("a", "c", "d", "e").forEach { name ->
            val line = source.lines().first { it.trim().startsWith("$name NUMBER") }
            val alone = ProjectDeclarationExtractor().extract(fileId, "CREATE PACKAGE p AS\n$line\nEND p;")
                .filterIsInstance<PackageValueDeclaration>().single()
            val expected = together.first { it.name == alone.name }
            assertThat(alone.typeRef.name).isEqualTo(expected.typeRef.name)
            assertThat(alone.constant).isEqualTo(expected.constant)
        }
    }
}
