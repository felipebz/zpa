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

/** The datatype/TypeRef syntax components applied to hand-lexed datatype slices. */
class TypeSyntaxTest {
    private val fileId = FileId("types.sql")

    /** Lexes [text] and returns the stream plus the exclusive end index of all real tokens (before EOF). */
    private fun lex(text: String): Pair<TokenStream, Int> {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text)
        return TokenStream(fileId, tokens) to tokens.size - 1
    }

    private fun accepts(text: String): Boolean {
        val (stream, end) = lex(text)
        return DatatypeShape(stream).strictDatatype(0, end)
    }

    private fun ref(text: String) = lex(text).let { (stream, end) -> TypeReferences(stream).typeReference(0, end) }

    private fun identity(text: String) = (ref(text) as NamedTypeRef).headerTypeIdentity

    @Test
    fun datatypeShapeAcceptsOnlyTheStrictForms() {
        listOf(
            "NUMBER", "DATE", "p.t", "s.p.t", "\"Q\".\"t\"", "NUMBER(10,2)", "VARCHAR2(30 CHAR)", "TIMESTAMP(3) WITH TIME ZONE",
            "INTERVAL DAY(2) TO SECOND(6)", "DOUBLE PRECISION", "t.c%TYPE", "t%ROWTYPE", "REF o", "REF s.o", "VARCHAR2 (10)"
        ).forEach { assertThat(accepts(it)).describedAs(it).isTrue() }
        listOf(
            "NUMBER b", "NUMBER(10", "NUMBER(10]", "t%FOO", "t%TYPE x", "REF t%TYPE", "REF", "p.", "1", "NUMBER FOO", "t.c%", "(10)"
        ).forEach { assertThat(accepts(it)).describedAs(it).isFalse() }
        val (stream, _) = lex("NUMBER")
        assertThat(DatatypeShape(stream).strictDatatype(0, 0)).isFalse()
        assertThat(DatatypeShape(stream).strictDatatype(1, 0)).isFalse()
    }

    @Test
    fun typeReferencesCarryNameAnchorAndExactRange() {
        val named = ref("p.t") as NamedTypeRef
        assertThat(named.name.toString()).isEqualTo("p.t")
        assertThat(named.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 3))
        val quoted = ref("\"Mixed\".t") as NamedTypeRef
        assertThat(quoted.name.segments.map { it.originalSpelling }).containsExactly("\"Mixed\"", "t")
        val type = ref("t.c%TYPE") as AnchoredTypeRef
        assertThat(type.anchor).isEqualTo(TypeAnchor.TYPE)
        assertThat(type.name.toString()).isEqualTo("t.c")
        assertThat(type.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 8))
        assertThat((ref("t%ROWTYPE") as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.ROWTYPE)
        val reference = ref("REF s.o") as RefTypeRef
        assertThat(reference.name.toString()).isEqualTo("s.o")
        assertThat(reference.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 7))
        assertThat(ref("NUMBER(10,2)")!!.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 12))
        assertThat(ref("1")).isNull()
        val (stream, _) = lex("NUMBER")
        assertThat(TypeReferences(stream).typeReference(0, 0)).isNull()
        assertThat(TypeReferences(stream).typeReference(5, 99)).isNull()
    }

    @Test
    fun parameterizedTypesKeepDistinctHeaderIdentitiesEvenWhenTypeRefsAreEqual() {
        val a = ref("VARCHAR2(10)") as NamedTypeRef
        val b = ref("VARCHAR2(20)") as NamedTypeRef
        // Heading-only syntax is not part of TypeRef equality, hashing or toString...
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a.toString()).isEqualTo(b.toString())
        // ...but it is retained and distinguishes the two headings.
        assertThat(a.headerTypeIdentity).isNotNull().isNotEqualTo(b.headerTypeIdentity)
        assertThat(identity("NUMBER(1,2)")).isNotEqualTo(identity("NUMBER(12)"))
        assertThat(identity("VARCHAR2(10 CHAR)")).isNotEqualTo(identity("VARCHAR2(10 BYTE)"))
        assertThat(identity("TIMESTAMP(3) WITH TIME ZONE")).isNotEqualTo(identity("TIMESTAMP(3) WITH LOCAL TIME ZONE"))
        // A plain single name carries no header identity.
        assertThat(identity("t")).isNull()
        assertThat(identity("\"A\"")).isNull()
        // Identity is built from tokens only: whitespace is trivia, unreserved words are case-insensitive.
        assertThat(identity("VARCHAR2 ( 10 )")).isEqualTo(identity("varchar2(10)"))
        assertThat(identity("VARCHAR2(10)")).isEqualTo(identity("VARCHAR2(10)"))
    }

    @Test
    fun typeShapeIsDerivedFromWordsInTheDeclarationRange() {
        fun shape(text: String): ProjectTypeShape? = lex(text).let { (stream, end) -> TypeReferences(stream).typeShape(0, end - 1) }
        assertThat(shape("TYPE r IS RECORD (a NUMBER);")).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(shape("TYPE o AS OBJECT (a NUMBER);")).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(shape("TYPE t IS TABLE OF NUMBER;")).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(shape("TYPE t IS VARRAY(3) OF NUMBER;")).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(shape("TYPE c IS REF CURSOR;")).isEqualTo(ProjectTypeShape.REF_CURSOR)
        assertThat(shape("TYPE s IS NUMBER;")).isNull()
    }
}
