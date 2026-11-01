//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiteralTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.LITERAL)
    }

    @Test
    fun matchesEveryLiteralBranch() {
        listOf(
            "NULL",
            "TRUE",
            "FALSE",
            "42",
            "42.5",
            "BINARY_DOUBLE_INFINITY",
            "BINARY_DOUBLE_NAN",
            "BINARY_FLOAT_INFINITY",
            "BINARY_FLOAT_NAN",
            "'text'",
            "DATE '2026-09-05'",
            "TIMESTAMP '2026-09-05 12:34:56'",
            "TIME '19:00:00'",
            "INTERVAL '4' YEAR",
            "\$\$PLSQL_UNIT"
        ).forEach { assertThat(p).matches(it) }
    }

    @Test
    fun preservesLiteralBoundaries() {
        assertThat(p).notMatches("42 + 1")
        assertThat(p).notMatches("DATE")
        assertThat(p).notMatches("INTERVAL '4'")
        assertThat(p).notMatches("UNKNOWN")
        assertThat(p).notMatches("TIME")
        assertThat(p).matches("TIME 'x'")
        assertThat(p).matches("TIME '19:00'")
        assertThat(p).notMatches("TIME 1")
        assertThat(p).notMatches("TIME (3) '19:00:00'")
        assertThat(p).notMatches("TIMEVALUE '19:00:00'")
    }

    @Test
    fun keepsTimeLiteralAsOneTokenUnderLiteral() {
        val literal = p.parse("TIME '19:00:00 +03:00'")
        org.assertj.core.api.Assertions.assertThat(literal.hasDirectChildren(PlSqlTokenType.TIME_LITERAL)).isTrue()
        org.assertj.core.api.Assertions.assertThat(literal.tokens.map { it.originalValue }).containsExactly("TIME '19:00:00 +03:00'")
    }
}
