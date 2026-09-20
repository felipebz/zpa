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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
