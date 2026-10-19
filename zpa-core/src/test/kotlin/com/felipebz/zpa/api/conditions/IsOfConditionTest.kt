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
