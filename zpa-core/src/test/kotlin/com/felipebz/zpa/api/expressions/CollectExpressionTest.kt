//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.asSemantic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CollectExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleCollect() {
        assertThat(p).matches("collect(foo)")
    }

    @Test
    fun matchesCollectWithDistinct() {
        assertThat(p).matches("collect(distinct foo)")
    }

    @Test
    fun matchesCollectWithUnique() {
        assertThat(p).matches("collect(unique foo)")
    }

    @Test
    fun matchesCollectWithDistinctAndOrderBy() {
        assertThat(p).matches("collect(distinct foo order by bar)")
    }

    @Test
    fun matchesCollectWithAll() {
        assertThat(p).matches("collect(all foo)")
    }

    @Test
    fun matchesCollectWithMultipleOrderByItems() {
        assertThat(p).matches("collect(foo order by sort_key asc nulls last, tie_breaker desc)")
    }

    @Test
    fun matchesCollectFilter() {
        val collect = p.parse("collect(value) filter (where value > 1)")

        assertThatAst(collect.getDescendants(AggregateSqlFunctionsGrammar.COLLECT_EXPRESSION)).hasSize(1)
        val filter = collect.getFirstDescendant(AggregateSqlFunctionsGrammar.FILTER_CLAUSE)
        assertThatAst(filter.asSemantic().type).isEqualTo(AggregateSqlFunctionsGrammar.FILTER_CLAUSE)
    }

    @Test
    fun matchesCollectAnalyticClause() {
        assertThat(p).matches("collect(value) over (partition by group_id order by sort_key)")
    }

    @Test
    fun matchesCollectFilterWithAnalyticClause() {
        assertThat(p).matches(
            "collect(value) filter (where value > 1) over (partition by group_id)"
        )
    }

    @Test
    fun rejectsCollectFilterAfterAnalyticClause() {
        assertThat(p).notMatches(
            "collect(value) over (partition by group_id) filter (where value > 1)"
        )
    }

    @Test
    fun rejectsCollectOrderSiblingsBy() {
        assertThat(p).notMatches("collect(value order siblings by sort_key)")
    }

    @Test
    fun rejectsIncompleteCollect() {
        listOf(
            "collect(",
            "collect(value order by)",
            "collect(value filter (where))"
        ).forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }
}
