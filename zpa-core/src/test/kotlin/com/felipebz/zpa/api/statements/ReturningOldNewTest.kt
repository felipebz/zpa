//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class ReturningOldNewTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.UPDATE_STATEMENT)
    }

    private fun update(returning: String) = "update t set c1 = 1 returning $returning;"

    @Test
    fun matchesOldAndNewOperands() {
        listOf(
            "old c1 into a", "new c1 into a", "old c1, new c1 into a, b", "old c1, new c2, c3 bulk collect into la, lb, lc",
            "old 1 into a", "new 1 into a", "old 'x' into c", "old null into a", "old t.c1 into a", "new t.c1 into a",
            "old s.t.c1 into a",
        ).forEach { assertThat(p).describedAs(it).matches(update(it)) }
        assertThat(p).matches("update t set c1 = 1 return old c1 into a;")
    }

    @Test
    fun matchesOldAndNewInsideLargerExpressions() {
        listOf(
            "c1 + old c2 into a", "old c1 + new c2 into a", "old c1 + old c2 into a", "old c1 + c2 into a",
            "c1 + old c2 + new c1 into a", "c1 * old c2 into a", "old c1 * 2 into a", "old c1 - new c1 into a",
            "old c1 || 'x' into c", "nvl(old c1, 0) into a", "nvl(old c1, new c1) into a", "abs(old c1) + 1 into a",
            "max(old c1) into a", "avg(old c1) into a", "decode(old c1, 1, 2, new c1) into a", "to_char(old c1) into c",
            "case when old c1 > 1 then new c1 else 0 end into a", "(old c1) into a", "(old c1 + 1) into a",
            "-old c1 into a", "- old c1 + 1 into a",
        ).forEach { assertThat(p).describedAs(it).matches(update(it)) }
    }

    @Test
    fun matchesInsertAndDelete() {
        setRootRule(PlSqlGrammar.INSERT_STATEMENT)
        assertThat(p).matches("insert into t (c1) values (1) returning old c1 into a;")
        assertThat(p).matches("insert into t (c1) values (1) returning c1 + new c1 into a;")
        setRootRule(PlSqlGrammar.DELETE_STATEMENT)
        assertThat(p).matches("delete from t returning new c1 into a;")
        assertThat(p).matches("delete from t returning old c1, c2 bulk collect into la, lb;")
    }

    @Test
    fun keepsOrdinaryReturningLists() {
        listOf("c1 into a", "c1, c2 into a, b", "c1 + c2 into a", "nvl(c1, 0) into a", "c1, c2 bulk collect into la, lb")
            .forEach { assertThat(p).describedAs(it).matches(update(it)) }
    }

    @Test
    fun rejectsMalformedModifiers() {
        listOf(
            "old new c1 into a", "old old c1 into a", "new old c1 into a", "old c1 + into a", "old c1 old into a", "old c1 as x into a",
            "old c1;",
        ).forEach { assertThat(p).describedAs(it).notMatches(update(it)) }
    }

    @Test
    fun treatsUnquotedOldAndNewAsModifiersInsideReturning() {
        listOf(
            "old into a", "new into a", "old, new into a, b", "c1, old into a, b", "old, c1 into a, b", "old(c1) into a",
            "old (c1) into a", "new(c1) into a", "old + 1 into a", "old - c1 into a", "old * 2 into a", "new + 1 into a",
            "new - c1 into a", "new * 2 into a", "c1 + old into a", "c1 + new into a", "nvl(old, 0) into a",
            "old c1 + old into a",
        ).forEach { assertThat(p).describedAs(it).notMatches(update(it)) }
    }

    @Test
    fun keepsQuotedAndQualifiedOldAndNewAsColumns() {
        listOf(
            "\"OLD\" into a", "\"NEW\" into a", "\"OLD\", \"NEW\" into a, b", "\"OLD\" + 1 into a",
            "old \"OLD\" into a", "old q.\"OLD\" into a", "q.old into a", "q.new into a", "old old into a", "old new into a",
            "new old into a", "c1 + old new into a", "nvl(old old, 0) into a",
        ).forEach { assertThat(p).describedAs(it).matches(update(it)) }
    }

    @Test
    fun keepsOldAndNewOrdinaryOutsideReturning() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf("old", "new", "old + 1", "old(c1)", "nvl(old, 0)", "c1 + new").forEach {
            assertThat(p).describedAs(it).matches(it)
        }
        setRootRule(PlSqlGrammar.UPDATE_STATEMENT)
        assertThat(p).matches("update t set old = new + 1 where old = 1;")
        assertThat(p).matches("update t set c1 = 1 where old > new;")
    }

    @Test
    fun doesNotAdmitModifiersOutsideReturning() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf("old c1", "1 + old c1", "nvl(old c1, 0)").forEach { assertThat(p).describedAs(it).notMatches(it) }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).notMatches("begin x := old c1; end;")
        assertThat(p).notMatches("begin execute immediate 's' returning old c1 into a; end;")
    }

    @Test
    fun buildsBlockWithOldNewReturning() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "declare a number; begin update t set s = s * 2 returning old s, new s, n bulk collect into x, y, z; end;")
        val values = tree.getDescendants(PlSqlGrammar.RETURNING_VALUE_EXPRESSION)
        assertThatAst(values.map { it.tokens.map { t -> t.originalValue } }).containsExactly(
            listOf("old", "s"), listOf("new", "s"))
        val nested = p.parse("begin update t set s = 1 returning s + old s into a; end;")
            .getFirstDescendant(PlSqlGrammar.RETURNING_VALUE_EXPRESSION)
        assertThatAst(nested.tokens.map { it.originalValue }).containsExactly("old", "s")
        assertThatAst(nested.parent.type).isNotEqualTo(PlSqlGrammar.RETURNING_VALUE_EXPRESSION)
    }
}
