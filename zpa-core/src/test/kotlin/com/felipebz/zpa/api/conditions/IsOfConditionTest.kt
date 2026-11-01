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
import org.assertj.core.api.Assertions.assertThat as assertThatValue

class IsOfConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    private fun typesOf(text: String): List<com.felipebz.flr.api.AstNodeType> {
        val condition = p.parse(text).getFirstDescendant(ConditionsGrammar.IS_OF_CONDITION)!!
        val children = condition.children
        val inside = children.drop(children.indexOfFirst { it.tokenValue == "(" } + 1).map { it.type }
        assertThatValue(inside).doesNotContain(PlSqlGrammar.OBJECT_REFERENCE, PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
        return condition.getChildren(PlSqlGrammar.DATATYPE).map { it.firstChild.type }
    }

    @Test
    fun matchesSimpleIsOf() {
        assertThat(p).matches("foo is of (bar)")
        assertThatValue(typesOf("foo is of (bar)")).containsExactly(PlSqlGrammar.CUSTOM_DATATYPE)
    }

    @Test
    fun matchesSimpleIsNotOf() {
        assertThat(p).matches("foo is not of (bar)")
    }

    @Test
    fun matchesIsOfType() {
        assertThat(p).matches("foo is of type (bar)")
    }

    @Test
    fun matchesIsNotOfType() {
        assertThat(p).matches("foo is not of type (bar)")
    }

    @Test
    fun matchesIsOfMultipleType() {
        assertThat(p).matches("foo is of type (bar, baz, foobar)")
    }

    @Test
    fun matchesIsOfOnly() {
        assertThat(p).matches("foo is of type (only bar)")
        assertThatValue(typesOf("foo is of type (only bar, s.baz)"))
            .containsExactly(PlSqlGrammar.CUSTOM_DATATYPE, PlSqlGrammar.CUSTOM_DATATYPE)
        assertThat(p).matches("foo is of (only bar, only baz)")
    }

    @Test
    fun matchesBuiltInTypes() {
        // PL/SQL parses a built-in name and fails only on compile (PLS-00382).
        assertThat(p).matches("foo is of (number)")
        assertThat(p).matches("foo is of (date)")
        assertThatValue(typesOf("foo is of (json)")).containsExactly(PlSqlGrammar.JSON_DATATYPE)
    }

    @Test
    fun notMatchesShapesBothParsersReject() {
        assertThat(p).notMatches("foo is of (ref bar)")
        assertThat(p).notMatches("foo is of (bar(10))")
        assertThat(p).notMatches("foo is of (t.c%type)")
        assertThat(p).notMatches("foo is of (bar@lnk)")
        assertThat(p).notMatches("foo is of (varchar2(10))")
    }

}
