//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class OverlapsConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesDatePairs() {
        assertThat(p).matches(
            "(date '2026-01-01', date '2026-01-10') overlaps " +
                "(date '2026-01-05', date '2026-01-15')"
        )
    }

    @Test
    fun matchesDateAndIntervalPairs() {
        assertThat(p).matches(
            "(date '2026-01-01', interval '10' day) overlaps " +
                "(date '2026-01-05', date '2026-01-15')"
        )
    }

    @Test
    fun matchesArbitraryExpressions() {
        assertThat(p).matches("(sysdate, sysdate + 2) overlaps (sysdate + 1, sysdate + 3)")
    }

    @Test
    fun matchesNumericPairs() {
        val source = "(0, 5) overlaps (3, 12)"

        assertThat(p).matches(source)
        val node = p.parse(source)
        assertThatAst(node.getDescendants(ConditionsGrammar.OVERLAPS_CONDITION)).hasSize(1)
    }

    @Test
    fun rejectsPairsWithWrongElementCount() {
        listOf(
            "(0) overlaps (1, 2)",
            "(0, 1) overlaps (2)",
            "(0, 1, 2) overlaps (3, 4)",
            "(0, 1) overlaps (2, 3, 4)"
        ).forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun matchesPrefixNot() {
        assertThat(p).matches("not ((0, 1) overlaps (2, 3))")
    }

    @Test
    fun rejectsInfixNot() {
        assertThat(p).notMatches("(0, 1) not overlaps (2, 3)")
    }
}
