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
package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtPlSqlAnnotationParserTest {

    @Test
    fun recognizesAnAnnotationWithoutAnArgument() {
        val token = token("--%test")

        val annotation = UtPlSqlAnnotationParser.parse(token, "%test")

        assertThat(annotation).isNotNull
        assertThat(annotation!!.kind).isEqualTo(UtPlSqlAnnotationKind.TEST)
        assertThat(annotation.name).isEqualTo("test")
        assertThat(annotation.argument).isNull()
        assertThat(annotation.token).isSameAs(token)
    }

    @Test
    fun recognizesAnAnnotationWithAnArgument() {
        val annotation = UtPlSqlAnnotationParser.parse(
            token("-- %test(Does something: use (a, b))"),
            " %test(Does something: use (a, b))"
        )

        assertThat(annotation).isNotNull
        assertThat(annotation!!.kind).isEqualTo(UtPlSqlAnnotationKind.TEST)
        assertThat(annotation.argument).isEqualTo("Does something: use (a, b)")
    }

    @Test
    fun normalizesEmptyArgumentsToNull() {
        val annotations = listOf("--%test", "--%test()", "--%test(   )")
            .map { source ->
                UtPlSqlAnnotationParser.parse(
                    token(source),
                    source.removePrefix("--")
                )
            }

        assertThat(annotations).allSatisfy { annotation ->
            assertThat(annotation).isNotNull
            assertThat(annotation!!.kind).isEqualTo(UtPlSqlAnnotationKind.TEST)
            assertThat(annotation.argument).isNull()
        }
    }

    @Test
    fun annotationNamesAreCaseInsensitiveAndArgumentsAreTrimmed() {
        val annotation = UtPlSqlAnnotationParser.parse(
            token("-- %RoLlBaCk(  foo  )"),
            " %RoLlBaCk(  foo  )"
        )

        assertThat(annotation).isNotNull
        assertThat(annotation!!.name).isEqualTo("rollback")
        assertThat(annotation.kind).isEqualTo(UtPlSqlAnnotationKind.ROLLBACK)
        assertThat(annotation.argument).isEqualTo("foo")
    }

    @Test
    fun acceptsTheWhitespaceUsedBetweenCommentAndAnnotation() {
        val annotation = UtPlSqlAnnotationParser.parse(
            token("--   %suite   "),
            "   %suite   "
        )

        assertThat(annotation).isNotNull
        assertThat(annotation!!.kind).isEqualTo(UtPlSqlAnnotationKind.SUITE)
    }

    @Test
    fun preservesUnknownAnnotationsWithoutTreatingThemAsErrors() {
        val annotation = UtPlSqlAnnotationParser.parse(
            token("--%future_annotation(value)"),
            "%future_annotation(value)"
        )

        assertThat(annotation).isNotNull
        assertThat(annotation!!.kind).isEqualTo(UtPlSqlAnnotationKind.UNKNOWN)
        assertThat(annotation.name).isEqualTo("future_annotation")
        assertThat(annotation.argument).isEqualTo("value")
    }

    @Test
    fun ignoresUnrelatedAndNonLineComments() {
        assertThat(UtPlSqlAnnotationParser.parse(token("-- ordinary comment"), " ordinary comment")).isNull()
        assertThat(UtPlSqlAnnotationParser.parse(token("/*%test*/"), "%test")).isNull()
        assertThat(UtPlSqlAnnotationParser.parse(token("-- value %test"), " value %test")).isNull()
    }

    @Test
    fun retainsAnnotationNamesWhenParametersAreMalformedOrIgnored() {
        val unclosed = UtPlSqlAnnotationParser.parse(token("--%test(unclosed"), "%test(unclosed")
        val missingBrackets = UtPlSqlAnnotationParser.parse(
            token("--%suite Description without brackets"),
            "%suite Description without brackets"
        )

        assertThat(unclosed).isNotNull
        assertThat(unclosed!!.kind).isEqualTo(UtPlSqlAnnotationKind.TEST)
        assertThat(unclosed.argument).isNull()
        assertThat(missingBrackets).isNotNull
        assertThat(missingBrackets!!.kind).isEqualTo(UtPlSqlAnnotationKind.SUITE)
        assertThat(missingBrackets.argument).isNull()
        assertThat(UtPlSqlAnnotationParser.parse(token("--%"), "%")).isNull()
    }

    private fun token(value: String): Token = Token.builder()
        .setType(GenericTokenType.IDENTIFIER)
        .setValueAndOriginalValue(value)
        .setLine(1)
        .setColumn(0)
        .build()
}
