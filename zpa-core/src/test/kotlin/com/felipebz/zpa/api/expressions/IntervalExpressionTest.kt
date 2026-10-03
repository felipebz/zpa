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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntervalExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesDayToSecondQualifiers() {
        listOf(
            "(a - b) day to second", "(a - b) day(9) to second", "(a - b) day to second(9)", "(a - b) day(9) to second(9)",
            "(a - b) day(0) to second", "(a - b) day(1) to second(0)", "(a - b) DAY (3) TO SECOND (3)",
            "(systimestamp - order_date) DAY(9) TO SECOND",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesYearToMonthQualifiers() {
        listOf(
            "(a - b) year to month", "(a - b) year(9) to month", "(a - b) year(0) to month", "(sysdate - sysdate) YEAR TO MONTH",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun acceptsOperandsOracleValidatesAfterParsing() {
        listOf(
            "(a) day to second", "(a + b) day to second", "(a - b - c) day to second", "(1 - 2) day to second",
            "('a' - 'b') day to second", "((a - b)) day to second", "((a - b) day to second)", "((a - b) day to second) day to second",
            "(nvl(a, b) - (c + 1)) day to second", "(date '2020-01-01' - date '2019-01-01') year to month",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun placesQualifiedExpressionsLikeAnyOperand() {
        listOf(
            "(a - b) day to second * 2", "2 * (a - b) day to second", "(a - b) day to second + 1", "-(a - b) day to second",
            "(a - b) day to second || 'x'", "(a - b) day to second = (c - d) day to second",
            "nvl((a - b) day to second, interval '1' day)", "extract(day from (a - b) day to second)",
            "case when (a - b) day(9) to second > interval '1' day then 1 else 0 end",
            "(a - b) day to second is null", "(a - b) day to second between x and y",
            "cast((a - b) day to second as interval day to second)", "to_char((a - b) day to second)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select (systimestamp - order_date) day(9) to second from orders where order_id = 2458;")
        assertThat(p).matches("select 1 from t where (a - b) year to month > interval '1' year;")
    }

    @Test
    fun rejectsMalformedQualifiers() {
        listOf(
            "(a - b) day to", "(a - b) to second", "(a - b) day second", "(a - b) day to month", "(a - b) year to second",
            "(a - b) month to year", "(a - b) second to day", "(a - b) hour to second", "(a - b) day to minute",
            "(a - b) day to second day to second", "(a - b) day to second year to month",
            "(a - b) day() to second", "(a - b) day(-1) to second", "(a - b) day(1.5) to second", "(a - b) day(:p) to second",
            "(a - b) day(1 + 1) to second", "(a - b) day(n) to second",
            "a - b day to second", "a day to second", "nvl(a, b) day to second", "sysdate day to second",
            "(a - b) at time zone 'UTC' day to second",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsIntervalQualifierNode() {
        val qualifier = p.parse("(a - b) day(9) to second(6)").getFirstDescendant(PlSqlGrammar.INTERVAL_QUALIFIER)
        assertThatAst(qualifier.tokens.map { it.originalValue }).containsExactly("day", "(", "9", ")", "to", "second", "(", "6", ")")
        val yearToMonth = p.parse("(a - b) year to month").getFirstDescendant(PlSqlGrammar.INTERVAL_QUALIFIER)
        assertThatAst(yearToMonth.tokens.map { it.originalValue }).containsExactly("year", "to", "month")
        assertThatAst(p.parse("(a - b)").getDescendants(PlSqlGrammar.INTERVAL_QUALIFIER)).isEmpty()
    }
}
