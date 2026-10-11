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

/** Sequence, standalone type and type body units, called directly on hand-lexed sources. */
class SchemaUnitExtractorTest {
    private val fileId = FileId("schema.sql")

    private fun extractor(text: String): Pair<SchemaUnitExtractor, TokenStream> {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text)
        val stream = TokenStream(fileId, tokens)
        return SchemaUnitExtractor(stream, TypeReferences(stream)) to stream
    }

    @Test
    fun sequenceUnitsReportDeclarationRangeAndNextIndex() {
        val (units, stream) = extractor("CREATE SEQUENCE app.s START WITH 1;\nCREATE SEQUENCE IF NOT EXISTS t;")
        val first = units.parseSequence(0, 1)!!
        val sequence = first.declarations.single() as SequenceDeclaration
        assertThat(sequence.name.toString()).isEqualTo("app.s")
        assertThat(sequence.kind).isEqualTo(ProjectDeclarationKind.SEQUENCE)
        assertThat(sequence.role).isEqualTo(DeclarationRole.STANDALONE)
        assertThat(sequence.fileId).isEqualTo(fileId)
        assertThat(sequence.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 35))
        // Resumes right after the terminating semicolon.
        assertThat(stream.isKeyword(first.nextIndex, "CREATE")).isTrue()
        val second = units.parseSequence(first.nextIndex, first.nextIndex + 1)!!
        assertThat((second.declarations.single() as SequenceDeclaration).name.toString()).isEqualTo("t")
        assertThat(second.declarations.single().sourceRange).isEqualTo(SourceRange(fileId, 2, 0, 2, 32))
    }

    @Test
    fun sequenceNamesAreLimitedToOneOrTwoSegmentsAndHaveNoCreatePrefixes() {
        assertThat(extractor("CREATE SEQUENCE a.b.c;").first.parseSequence(0, 1)).isNull()
        assertThat(extractor("CREATE OR REPLACE SEQUENCE s;").first.parseSequence(0, 3)).isNull()
        assertThat(extractor("CREATE SEQUENCE ;").first.parseSequence(0, 1)).isNull()
    }

    @Test
    fun standaloneTypesCarryShapeAndRange() {
        val (units, stream) = extractor("CREATE TYPE o AS OBJECT (a NUMBER);\nCREATE OR REPLACE TYPE t AS TABLE OF NUMBER;")
        val first = units.parseStandaloneType(0, 1)!!
        val type = first.declarations.single() as StandaloneTypeDeclaration
        assertThat(type.name.toString()).isEqualTo("o")
        assertThat(type.shape).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(type.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        assertThat(type.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 35))
        assertThat(stream.isKeyword(first.nextIndex, "CREATE")).isTrue()
        val index = first.nextIndex
        val second = units.parseStandaloneType(index, index + 3)!!
        assertThat((second.declarations.single() as StandaloneTypeDeclaration).shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(units.parseStandaloneType(0, 99)).isNull()
    }

    @Test
    fun typeBodiesYieldNoDeclarationsAndResumeAtTheNextCreate() {
        val (units, stream) = extractor("CREATE TYPE BODY t AS MEMBER FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END; END;\nCREATE SEQUENCE s;")
        val body = units.parseTypeBody(1)
        assertThat(body.declarations).isEmpty()
        assertThat(stream.isKeyword(body.nextIndex, "CREATE")).isTrue()
        assertThat(stream.isKeyword(body.nextIndex + 1, "SEQUENCE")).isTrue()
    }

    @Test
    fun independentSchemaUnitsInOneSourceKeepOrderAndIdentity() {
        val facts = ProjectDeclarationExtractor().extract(
            fileId,
            "CREATE SEQUENCE s1;\nCREATE TYPE t1 AS OBJECT (a NUMBER);\nCREATE TYPE BODY t1 AS MEMBER PROCEDURE p IS BEGIN NULL; END; END;\nCREATE SEQUENCE s2;"
        )
        assertThat(facts.map { it.kind to it.qualifiedName().toString() }).containsExactly(
            ProjectDeclarationKind.SEQUENCE to "s1",
            ProjectDeclarationKind.STANDALONE_TYPE to "t1",
            ProjectDeclarationKind.SEQUENCE to "s2"
        )
        assertThat(facts.map { it.fileId }).containsOnly(fileId)
    }
}
