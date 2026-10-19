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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatValue

class TreatAsExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun typeOf(text: String): AstNode {
        val treat = p.parse(text).getFirstDescendant(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION)!!
        val children = treat.children
        val type = children[children.indexOfFirst { it.tokenValue.equals("AS", ignoreCase = true) } + 1]
        assertThatValue(type.type).isEqualTo(PlSqlGrammar.DATATYPE)
        return type.firstChild
    }

    private fun assertType(text: String, expected: AstNodeType) {
        assertThatValue(typeOf(text).type).describedAs(text).isEqualTo(expected)
        val children = p.parse(text).getFirstDescendant(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION)!!.children
        val afterAs = children.drop(children.indexOfFirst { it.tokenValue.equals("AS", ignoreCase = true) } + 1).map { it.type }
        assertThatValue(afterAs).describedAs(text).doesNotContain(PlSqlGrammar.OBJECT_REFERENCE, PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
    }

    @Test
    fun matchesSimpleTreatAs() {
        assertThat(p).matches("treat(foo as bar)")
        assertType("treat(foo as bar)", PlSqlGrammar.CUSTOM_DATATYPE)
    }

    @Test
    fun matchesTreatAsQualifiedType() {
        assertThat(p).matches("treat(foo as s.bar)")
        assertType("treat(foo as s.bar)", PlSqlGrammar.CUSTOM_DATATYPE)
    }

    @Test
    fun matchesTreatAsRef() {
        assertThat(p).matches("treat(foo as ref bar)")
        assertThat(p).matches("treat(foo as ref s.bar)")
        assertType("treat(foo as ref bar)", PlSqlGrammar.REF_DATATYPE)
    }

    @Test
    fun matchesTreatAsRef2() {
        assertThat(p).matches("treat(foo as ref bar).test")
        assertType("treat(foo as ref bar).test", PlSqlGrammar.REF_DATATYPE)
    }

    @Test
    fun matchesTreatAsJson() {
        assertThat(p).matches("treat(foo as json)")
        assertType("treat(foo as json)", PlSqlGrammar.JSON_DATATYPE)
        // SQL accepts a JSON modifier list and fails only on the types (ORA-00932).
        assertThat(p).matches("treat(foo as json(object))")
        assertType("treat(foo as json(array))", PlSqlGrammar.JSON_DATATYPE)
    }

    @Test
    fun matchesTypeMarksThePlSqlParserTakes() {
        // PL/SQL parses these and fails only on compile (PLS-00725, PLS-00352).
        assertThat(p).matches("treat(foo as number)")
        assertThat(p).matches("treat(foo as timestamp with time zone)")
        assertThat(p).matches("treat(foo as interval day to second)")
        assertThat(p).matches("treat(foo as pls_integer)")
        assertType("treat(foo as t.c%type)", PlSqlGrammar.ANCHORED_DATATYPE)
        assertType("treat(foo as t%rowtype)", PlSqlGrammar.ANCHORED_DATATYPE)
        assertType("treat(foo as bar@lnk)", PlSqlGrammar.CUSTOM_DATATYPE)
        assertType("treat(foo as ref bar@lnk)", PlSqlGrammar.REF_DATATYPE)
    }

    @Test
    fun notMatchesConstrainedTypes() {
        assertThat(p).notMatches("treat(foo as bar(10))")
        assertThat(p).notMatches("treat(foo as varchar2(10))")
        assertThat(p).notMatches("treat(foo as number(5))")
        assertThat(p).notMatches("treat(foo as ref bar%type)")
        assertThat(p).notMatches("treat(foo as json limit 10)")
        assertThat(p).notMatches("treat(foo as json object)")
    }

    @Test
    fun matchesAsExpression() {
        assertThat(p).matches("(foo as bar)")
        assertType("(foo as bar)", PlSqlGrammar.CUSTOM_DATATYPE)
    }

    @Test
    fun matchesAsExpressionWithMethodCall() {
        assertThat(p).matches("(foo as bar).baz()")
        assertType("(foo as bar).baz()", PlSqlGrammar.CUSTOM_DATATYPE)
        // PL/SQL parses REF here and fails only on compile (PLS-00672).
        assertType("(self as ref bar).baz()", PlSqlGrammar.REF_DATATYPE)
    }

    @Test
    fun notMatchesAsExpressionWithConstraint() {
        assertThat(p).notMatches("(self as bar(10)).baz()")
    }

}
