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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.charset.StandardCharsets

class CheckUtilsTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value = [
            "null       | true  | false",
            "''         | true  | true",
            "N''        | true  | true",
            "q'[]'      | true  | true",
            "' '        | false | false",
            "q'[ ]'     | false | false",
            "(null)     | false | false",
            "null_value | false | false",
        ]
    )
    fun nullLiterals(expression: String, nullOrEmpty: Boolean, empty: Boolean) {
        val node = assignedValue(expression)
        assertThat(CheckUtils.isNullLiteralOrEmptyString(node)).isEqualTo(nullOrEmpty)
        assertThat(CheckUtils.isEmptyString(node)).isEqualTo(empty)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value = [
            "nvl(a, null)       | a",
            "nvl(a, '')         | a",
            "nvl(a, (null))     | nvl",
            "nvl(a, null_value) | nvl",
            "nvl(a, b, null)    | nvl",
        ]
    )
    fun nvlWithANullSecondArgumentIsSkipped(expression: String, kept: String) {
        assertThat(CheckUtils.skipNvlWithNull(assignedValue(expression)).tokenOriginalValue).isEqualTo(kept)
    }

    private fun assignedValue(expression: String): AstNode {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8)).parse("begin\n  x := $expression;\nend;\n/\n")
        val assignment = root.getFirstDescendant(PlSqlGrammar.ASSIGNMENT_STATEMENT)
        return assignment.children[assignment.children.indexOfFirst { it.type === PlSqlPunctuator.ASSIGNMENT } + 1]
    }

}
