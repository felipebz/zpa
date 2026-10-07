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

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar

class JsonValueTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonValueWithOnEmptyBeforeOnError() {
        assertThat(p).matches("json_value(doc, '$.id' null on empty null on error)")
    }

    @Test
    fun matchesJsonValueWithOnErrorBeforeOnEmpty() {
        assertThat(p).matches("json_value(doc, '$.id' null on error null on empty)")
    }

    @Test
    fun doesNotMatchJsonValueWithRepeatedOnErrorOrOnEmpty() {
        assertThat(p).notMatches("json_value(doc, '$.id' null on error error on error)")
        assertThat(p).notMatches("json_value(doc, '$.id' null on empty error on empty)")
        assertThat(p).notMatches("json_value(doc, '$.id' null on error null on empty error on error)")
    }

    @Test
    fun matchesJsonValue() {
        assertThat(p).matches("json_value(doc, '$')")
    }

    @Test
    fun matchesJsonValueFormatJson() {
        assertThat(p).matches("json_value('{}' format json, '$')")
    }

    @Test
    fun matchesJsonValueWithPassingClause() {
        assertThat(p).matches("json_value(doc, '$' passing 1 as x, 2 as y)")
    }

    @Test
    fun matchesJsonValueWithReturning() {
        assertThat(p).matches("json_value(doc, '$' returning clob)")
    }

    @Test
    fun matchesJsonValueWithReturningAscii() {
        assertThat(p).matches("json_value(doc, '$' returning clob ascii)")
    }

    @Test
    fun matchesLongJsonValue() {
        assertThat(p).matches("""
            json_value(doc, '$'
            passing 'a' as a
            returning varchar2(10) truncate
            error on error
            error on empty
            error on mismatch (missing data)
            ignore on mismatch (extra data)
            type (strict))""")
    }


    @Test
    fun matchesJsonValueWithPathExpression() {
        assertThat(p).matches("json_value(doc, '$.a[' || i || '].b' returning clob null on error)")
    }

    @Test
    fun doesNotMatchJsonValueWithoutPath() {
        assertThat(p).notMatches("json_value(doc,)")
    }

    private fun handlers(clauses: String) = "json_value(d, '$.a' returning number $clauses)"

    @Test
    fun matchesDocumentationHandlerCombination() {
        assertThat(p).matches(
            """json_value('{a:"cat"}','$.a.number()' NULL ON EMPTY
               ERROR ON MISMATCH DEFAULT -1 ON ERROR)""")
    }

    @Test
    fun matchesDefaultOperandsOracleChecksLater() {
        listOf("1", "-1", "+1", "- -1", "'x'", "null", "1.5e3", ":x", "dummy", "x.y", "(1)", "abs(1)", "sysdate", "date '2020-01-01'")
            .forEach {
                assertThat(p).describedAs(it).matches(handlers("default $it on error"))
                assertThat(p).describedAs(it).matches(handlers("default $it on empty"))
            }
    }

    @Test
    fun rejectsDefaultOperandsOracleParsesAsSyntaxError() {
        listOf("", "1 + 2", "1 || 2", "1 2", "-", "not 1", "exists (select 1 from dual)", "1 collate binary", "2 ** 2")
            .forEach { assertThat(p).describedAs(it).notMatches(handlers("default $it on error")) }
        assertThat(p).notMatches(handlers("default -1 on mismatch"))
    }

    @Test
    fun matchesHandlersInEveryOrderOracleAccepts() {
        listOf(
            "null on empty error on error", "error on error null on empty",
            "default -1 on error null on empty", "null on empty default -1 on error",
            "null on empty error on mismatch default -1 on error", "null on empty default -1 on error error on mismatch",
            "error on mismatch null on empty default -1 on error", "default -1 on error error on mismatch null on empty",
            "error on mismatch default -1 on error", "error on mismatch null on empty",
            "null on mismatch (missing data) error on mismatch (extra data)",
        ).forEach { assertThat(p).describedAs(it).matches(handlers(it)) }
    }

    @Test
    fun rejectsRepeatedAndMalformedHandlers() {
        listOf(
            "null on empty error on empty", "null on error error on error", "default 1 on error null on error",
            "null on empty error on empty error on error", "null on foo", "null error", "default 1 on", "on error",
            "error on mismatch (", "error on mismatch ()", "error on mismatch missing data", "default -1 on error ,",
        ).forEach { assertThat(p).describedAs(it).notMatches(handlers(it)) }
    }

    @Test
    fun buildsHandlerNodesWithoutExtraWrappers() {
        val call = p.parse(handlers("null on empty error on mismatch default -1 on error"))
            .getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_VALUE_EXPRESSION)!!
        assertThatAst(call.children.map { it.name }).containsSubsequence(
            "JSON_VALUE_RETURNING_CLAUSE", "JSON_VALUE_ERROR_EMPTY_CLAUSES")
        val wrapper = call.getFirstChild(SingleRowSqlFunctionsGrammar.JSON_VALUE_ERROR_EMPTY_CLAUSES)
        assertThatAst(wrapper.children.map { it.name }).containsExactly(
            "JSON_VALUE_ON_EMPTY_CLAUSE", "JSON_VALUE_ON_MISMATCH_CLAUSE", "JSON_VALUE_ON_ERROR_CLAUSE")
        val onError = wrapper.getFirstChild(SingleRowSqlFunctionsGrammar.JSON_VALUE_ON_ERROR_CLAUSE)
        assertThatAst(onError.children.map { it.name }).containsExactly("DEFAULT", "UNARY_EXPRESSION", "ON", "ERROR")
        assertThatAst(onError.getFirstChild(PlSqlGrammar.UNARY_EXPRESSION).children.map { it.name })
            .containsExactly("MINUS", "LITERAL")
        val plain = p.parse(handlers("default 1 on error")).getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_VALUE_ON_ERROR_CLAUSE)!!
        assertThatAst(plain.children.map { it.name }).containsExactly("DEFAULT", "LITERAL", "ON", "ERROR")
    }
}
