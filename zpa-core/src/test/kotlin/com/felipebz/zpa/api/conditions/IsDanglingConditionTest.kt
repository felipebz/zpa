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
package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IsDanglingConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesDanglingConditions() {
        listOf(
            "r is dangling", "r is not dangling", "R IS DANGLING", "o.customer_ref is not dangling",
            "(t.r) is dangling", "deref(t.r) is dangling", "nvl(t.r, null) is dangling", "null is dangling",
            "n + 1 is dangling", "t.n is dangling", "pkg.f(x).r is dangling",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun combinesWithOtherConditions() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf(
            "r is dangling and n = 1", "n = 1 or r is not dangling", "not r is dangling", "not (r is dangling)",
            "r is dangling or r is not dangling and 1 = 1", "(r is dangling) is null",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedDanglingConditions() {
        listOf(
            "r is not not dangling", "r is dangling null", "r is dangling dangling", "r dangling", "dangling",
            "r is dangling(1)", "r is \"DANGLING\"", "r is not", "r is", "r is null is dangling",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesInQueryPositions() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf(
            "case when r is dangling then 1 else 0 end",
            "case when r is not dangling then 1 end",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "select o.customer_ref.cust_email from oc_orders o where o.customer_ref is not dangling;",
            "select 1 from t group by r having max(n) > 1 and r is dangling;",
            "select 1 from dual where exists (select 1 from t where t.r is not dangling);",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun buildsDanglingConditionNode() {
        val negated = p.parse("t.r is not dangling").getFirstDescendant(ConditionsGrammar.IS_DANGLING_CONDITION)
        assertThatAst(negated.tokens.map { it.originalValue }).containsExactly("t", ".", "r", "is", "not", "dangling")
        val plain = p.parse("r is dangling").getFirstDescendant(ConditionsGrammar.IS_DANGLING_CONDITION)
        assertThatAst(plain.tokens.map { it.originalValue }).containsExactly("r", "is", "dangling")
        assertThatAst(p.parse("r is null").getDescendants(ConditionsGrammar.IS_DANGLING_CONDITION)).isEmpty()
    }
}
