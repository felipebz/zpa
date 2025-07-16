/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class AnalyticClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.ANALYTIC_CLAUSE)
    }

    @Test
    fun matchesSimpleOver() {
        assertThat(p).matches("over ()")
    }

    @Test
    fun matchesOverPartitionBy() {
        assertThat(p).matches("over (partition by foo)")
    }

    @Test
    fun matchesOverPartitionByWithMultipleExpressions() {
        assertThat(p).matches("over (partition by foo, bar, baz)")
    }

    @Test
    fun matchesOverOrderBy() {
        assertThat(p).matches("over (order by foo)")
    }

    @Test
    fun matchesOverWithWindowingUnboundedPreceding() {
        assertThat(p).matches("over (order by foo rows unbounded preceding)")
        assertThat(p).matches("over (order by foo range unbounded preceding)")
    }

    @Test
    fun matchesOverWithWindowingCurrentRow() {
        assertThat(p).matches("over (order by foo rows current row)")
    }

    @Test
    fun matchesOverWithWindowingExpressionPreceding() {
        assertThat(p).matches("over (order by foo rows 1 preceding)")
    }

    @Test
    fun matchesOverWithWindowingWithBetween() {
        assertThat(p).matches("over (order by foo rows between unbounded preceding and current row)")
    }

    @Test
    fun matchesLongAnalyticClause() {
        assertThat(p).matches("over (partition by foo order by foo rows between unbounded preceding and unbounded following)")
    }

    @Test
    fun matchesOverPartitionByExpression() {
        assertThat(p).matches("over (partition by foo + bar)")
    }

}
