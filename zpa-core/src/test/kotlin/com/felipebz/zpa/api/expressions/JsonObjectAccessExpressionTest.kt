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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonObjectAccessExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonObjectAccessForms() {
        val expressions = listOf(
            "t.data[0]",
            "t.data.a[0]",
            "t.data.a[*]",
            "t.data.a[*].sum()",
            "t.data[0].name",
            "t.data[0].items[1]",
            "\"t\".\"data\"[0]"
        )

        expressions.forEach { expression ->
            assertThat(p).matches(expression)
            val jsonNodes = p.parse(expression)
                .getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION)
            assertThat(jsonNodes)
                .describedAs(expression)
                .hasSize(1)
            assertThat(jsonNodes.single().tokens.joinToString("") { it.originalValue })
                .describedAs(expression)
                .isEqualTo(expression.substringBefore(".sum()"))
        }
    }

    @Test
    fun keepsOrdinarySharedPrefixesOffTheJsonPath() {
        val expressions = listOf(
            "a.b",
            "a.b.c",
            "a.b(1)",
            "package.function(1)",
            "table_name.column",
            "\"table\".column",
            "a.b.c.d",
            "t.data()"
        )

        expressions.forEach { expression ->
            assertThat(p).matches(expression)
            assertThat(p.parse(expression).getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION))
                .describedAs(expression)
                .isEmpty()
        }
    }

    @Test
    fun preservesParenthesisAndMalformedArrayBoundaries() {
        assertThat(p).matches("(t.data[0])")
        assertThat(p.parse("(t.data[0])").getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION))
            .hasSize(1)

        assertThat(p).notMatches("t.data[")
        assertThat(p).notMatches("t.data[]")
        assertThat(p).notMatches("t.data[0")
        assertThat(p).notMatches("t.data[0]()")
        assertThat(p).notMatches("t.data.a[0]()")
    }
}
