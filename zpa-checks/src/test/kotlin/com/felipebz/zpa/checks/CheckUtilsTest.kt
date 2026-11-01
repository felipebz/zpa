//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
