//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CollateExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesCollateOperator() {
        listOf(
            "s collate binary_ci", "s collate \"BINARY_CI\"", "s COLLATE GENERIC_M", "s collate using_nls_comp",
            "(s || 'x') collate binary_ci", "s || 'x' collate binary_ci", "'a' || s collate binary_ci",
            "upper(s) collate binary_ci", "-s collate binary_ci", "n * 2 collate binary_ci", "s collate binary_ci || 'x'",
            "s collate binary_ci = 'A'", "s = 'A' collate binary_ci", "s like 'A%' collate binary_ci",
            "s collate binary_ci like 'A%'", "s collate binary_ci is null", "s in ('a' collate binary_ci)",
            "s between 'a' collate binary_ci and 'z'", "nvl(s collate binary_ci, 'x')",
            "case s collate binary_ci when 'a' then 1 end", "substr(c, 1) collate generic_m",
            "count(*) over (partition by s collate binary_ci)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedCollate() {
        listOf(
            "s collate", "s collate 'binary_ci'", "s collate 1", "s collate (binary_ci)", "s collate sys.binary_ci",
            "s collate binary_ci collate binary_ai", "collate binary_ci", "s collate binary_ci at time zone 'UTC'",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun appliesCollateOnceAfterPrefixOperators() {
        listOf("-s collate a", "+s collate a", "- +s collate a", "-s at time zone 'UTC' collate a", "d at time zone 'UTC' collate a")
            .forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "-s collate a collate b", "+s collate a collate b", "- +s collate a collate b",
            "-s collate a at time zone 'UTC'",
            "prior s collate a", "prior s collate a collate b", "connect_by_root s collate a", "prior -s collate a",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        assertThat(p).matches("prior s")
        assertThat(p).matches("connect_by_root s")
        assertThat(p).matches("-s")
    }

    @Test
    fun attachesCollateAfterLiteralTimeZoneToTheWholeExpression() {
        listOf(
            "d at time zone 'UTC' collate a", "d at time zone ('UTC' collate a)", "d at time zone ('UTC' collate a) collate b",
            "d at time zone ('UTC' || '') collate a", "d at time zone z collate a", "d at time zone z collate a collate b",
            "d at time zone nvl(z, 'UTC') collate a", "d at time zone 'a' || 'b'", "d at time zone ('a' || 'b')",
            "d at time zone z || 'x'", "-s at time zone 'UTC' collate a",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "d at time zone 'UTC' collate a collate b", "-s at time zone 'UTC' collate a collate b",
            "d at time zone 'UTC' collate a at time zone 'UTC'",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }

        val whole = p.parse("d at time zone 'UTC' collate ci").getFirstDescendant(PlSqlGrammar.SUFFIXED_UNARY_EXPRESSION)
        assertThatAst(whole.children.drop(1).map { it.type }).containsExactly(
            PlSqlGrammar.AT_TIME_ZONE_EXPRESSION, PlSqlGrammar.COLLATE_EXPRESSION)
        assertThatAst(whole.firstChild.tokens.map { it.originalValue }).containsExactly("d")
        val zone = whole.getFirstChild(PlSqlGrammar.AT_TIME_ZONE_EXPRESSION)
        assertThatAst(zone.tokens.map { it.originalValue }).containsExactly("at", "time", "zone", "'UTC'")
        assertThatAst(zone.getDescendants(PlSqlGrammar.COLLATE_EXPRESSION)).isEmpty()

        val nested = p.parse("d at time zone ('UTC' collate ci)").getFirstDescendant(PlSqlGrammar.AT_TIME_ZONE_EXPRESSION)
        assertThatAst(nested.getDescendants(PlSqlGrammar.COLLATE_EXPRESSION)).hasSize(1)
        val column = p.parse("d at time zone z collate ci").getFirstDescendant(PlSqlGrammar.AT_TIME_ZONE_EXPRESSION)
        assertThatAst(column.getDescendants(PlSqlGrammar.COLLATE_EXPRESSION)).hasSize(1)
    }

    @Test
    fun attachesCollateToTheWholePrefixExpression() {
        val negated = p.parse("-s collate ci").getFirstDescendant(PlSqlGrammar.SUFFIXED_UNARY_EXPRESSION)
        assertThatAst(negated.type).isEqualTo(PlSqlGrammar.SUFFIXED_UNARY_EXPRESSION)
        assertThatAst(negated.children.map { it.type }).containsExactly(PlSqlGrammar.UNARY_EXPRESSION, PlSqlGrammar.COLLATE_EXPRESSION)
        assertThatAst(negated.firstChild.tokens.map { it.originalValue }).containsExactly("-", "s")
        assertThatAst(negated.getDescendants(PlSqlGrammar.COLLATE_EXPRESSION)).hasSize(1)
        assertThatAst(p.parse("-s").getDescendants(PlSqlGrammar.SUFFIXED_UNARY_EXPRESSION)).isEmpty()
        assertThatAst(p.parse("-s").getFirstDescendant(PlSqlGrammar.UNARY_EXPRESSION).tokens.map { it.originalValue }).containsExactly("-", "s")
        assertThatAst(p.parse("prior s").getFirstDescendant(PlSqlGrammar.UNARY_EXPRESSION).tokens.map { it.originalValue }).containsExactly("prior", "s")
    }

    @Test
    fun matchesCollateInQueryClauses() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "select last_name from employees order by last_name collate generic_m;",
            "select * from dual order by a nulls first, b nulls last, substr(c, 1) collate generic_m",
            "select * from t order by s collate binary_ci desc nulls last;",
            "select * from t where s collate binary_ci = 'A';",
            "select s collate binary_ci x from t;", "select s collate binary_ci as x, n from t;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun buildsCollateNodeAtOperandLevel() {
        val tree = p.parse("a || b collate binary_ci")
        val collate = tree.getFirstDescendant(PlSqlGrammar.COLLATE_EXPRESSION)
        assertThatAst(collate.tokens.map { it.originalValue }).containsExactly("collate", "binary_ci")
        assertThatAst(collate.parent.tokens.map { it.originalValue }).containsExactly("b", "collate", "binary_ci")
        assertThatAst(p.parse("a || b").getDescendants(PlSqlGrammar.COLLATE_EXPRESSION)).isEmpty()
    }
}
