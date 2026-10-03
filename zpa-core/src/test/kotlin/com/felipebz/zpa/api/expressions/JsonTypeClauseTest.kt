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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class JsonTypeClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun matches(vararg expressions: String) {
        for (e in expressions) assertThat(p).describedAs(e).matches(e)
    }

    private fun notMatches(vararg expressions: String) {
        for (e in expressions) assertThat(p).describedAs(e).notMatches(e)
    }

    @Test
    fun matchesParenthesisedTypeInEachFunction() {
        for (mode in listOf("strict", "lax", "STRICT", "LAX")) {
            matches(
                "json_exists(d, '\$.a' type($mode))", "json_value(d, '\$.a' type ($mode))",
                "json_query(d, '\$.a' type($mode))", "json_transform(d, set '\$.a' = 2 type($mode))"
            )
        }
        matches("json_exists(d, '\$.a' type ( strict ))", "json_value(d, '\$.a' type ( lax ))")
    }

    @Test
    fun matchesTypeAsTheLastClause() {
        matches(
            "json_exists(d, '\$.a' passing 1 as \"d\" type(strict))", "json_exists(d, '\$.a' error on error type(strict))",
            "json_exists(d, '\$.a' true on empty type(strict))",
            "json_exists(d, '\$.a' passing 1 as \"d\" error on error true on empty type(lax))",
            "json_value(d, '\$.a' returning number type(strict))", "json_value(d, '\$.a' null on error type(strict))",
            "json_value(d, '\$.a' passing 1 as \"d\" type(lax))", "json_value(d, '\$.a' error on mismatch type(lax))",
            "json_value(d, '\$.a' null on empty type(strict))",
            "json_value(d, '\$.a' returning number error on error error on mismatch type(strict))",
            "json_query(d, '\$.a' returning varchar2 type(strict))", "json_query(d, '\$.a' with wrapper type(strict))",
            "json_query(d, '\$.a' error on error type(strict))", "json_query(d, '\$.a' null on empty type(strict))",
            "json_query(d, '\$.a' error on mismatch type(lax))", "json_query(d, '\$.a' passing 1 as \"d\" type(lax))",
            "json_query(d, '\$.a' passing 1 as \"d\" returning varchar2 with wrapper keep quotes error on error null on empty " +
                "error on mismatch type(strict))",
            "json_transform(d, set '\$.a' = 2 returning varchar2 type(strict))", "json_transform(d, set '\$.a' = 2 pretty type(strict))",
            "json_transform(d, set '\$.a' = 2 ascii type(strict))", "json_transform(d, set '\$.a' = 2 passing 1 as \"d\" type(strict))",
            "json_transform(d, set '\$.a' = 2 returning varchar2 pretty ascii type(lax))",
            "json_transform(d, set '\$.a' = 2 passing 1 as \"d\" returning varchar2 pretty ascii type(lax))",
            "json_transform(d, insert '\$.b' = path '\$?(@.a > 0).a + 1' type(lax))"
        )
    }

    @Test
    fun rejectsUnparenthesisedAndMalformedTypeClauses() {
        for (f in listOf("json_exists(d, '\$.a' %s)", "json_value(d, '\$.a' %s)", "json_query(d, '\$.a' %s)",
            "json_transform(d, set '\$.a' = 2 %s)")) {
            notMatches(
                f.format("type strict"), f.format("type lax"), f.format("type"), f.format("type ()"), f.format("type (strict, lax)"),
                f.format("type (strict"), f.format("type strict)"), f.format("type((strict))"), f.format("type ('strict')"),
                f.format("type(strict) type(lax)"), f.format("type strict type lax")
            )
        }
    }

    @Test
    fun rejectsTypeBeforeOtherClauses() {
        notMatches(
            "json_exists(d, '\$.a' type(strict) passing 1 as \"d\")", "json_exists(d, '\$.a' type(strict) error on error)",
            "json_exists(d, '\$.a' type(strict) true on empty)", "json_exists(d, '\$.a' error on error type(lax) true on empty)",
            "json_value(d, '\$.a' type(strict) returning number)", "json_value(d, '\$.a' type(strict) null on error)",
            "json_value(d, '\$.a' type(lax) passing 1 as \"d\")", "json_value(d, '\$.a' type(lax) error on mismatch)",
            "json_value(d, '\$.a' type(strict) null on empty)",
            "json_query(d, '\$.a' type(strict) returning varchar2)", "json_query(d, '\$.a' type(strict) with wrapper)",
            "json_query(d, '\$.a' type(strict) error on error)", "json_query(d, '\$.a' type(lax) error on mismatch)",
            "json_query(d, '\$.a' type(lax) passing 1 as \"d\")",
            "json_transform(d, set '\$.a' = 2 type(strict) returning varchar2)", "json_transform(d, set '\$.a' = 2 type(strict) pretty)",
            "json_transform(d, set '\$.a' = 2 type(strict) passing 1 as \"d\")",
            "json_transform(d, set '\$.a' = 2 returning varchar2 passing 1 as \"d\")",
            "json_transform(d, set '\$.a' = 2 pretty passing 1 as \"d\")",
            "json_transform(d, set '\$.a' = 2 returning varchar2 pretty ascii passing 1 as \"d\" type(lax))",
            "json_transform(d, set '\$.a' = 2 passing 1 as \"d\" passing 2 as \"e\")"
        )
    }

    @Test
    fun rejectsTypeInJsonTable() {
        matches("json_table(d, '\$' error on error columns (a number path '\$.a'))")
        notMatches(
            "json_table(d, '\$' type(strict) columns (a number path '\$.a'))", "json_table(d, '\$' type(lax) columns (a number path '\$.a'))",
            "json_table(d, '\$' error on error type(strict) columns (a number path '\$.a'))",
            "json_table(d, '\$' type(strict) error on error columns (a number path '\$.a'))",
            "json_table(d, '\$' error on error type(strict) null on empty columns (a number path '\$.a'))",
            "json_table(d, '\$' columns (a number path '\$.a') type(strict))", "json_table(d, '\$' type strict columns (a number path '\$.a'))"
        )
    }

    @Test
    fun matchesParenthesisedStrictSyntaxInIsJson() {
        matches(
            "d is json (strict)", "d is json (lax)", "d is json ( strict )", "d is not json (strict)", "d is not json (lax)",
            "d is json format json (strict)", "d is json value (strict)", "d is json (value) (strict)", "d is json (value, array)",
            "d is json strict", "d is json lax", "d is json strict allow scalars with unique keys", "d is json format json lax"
        )
        notMatches(
            "d is json (strict) (strict)", "d is json (strict) with unique keys",
            "d is json with unique keys (strict)", "d is json (lax) without unique keys", "d is json (strict) format json",
            "d is json allow scalars (strict)", "d is json (strict) allow scalars", "d is json (strict) disallow scalars with unique keys",
            "d is json strict (strict)", "d is json (strict) strict", "d is json (strict) validate '{}'", "d is json validate '{}' (strict)",
            "d is json type(strict)"
        )
    }

    @Test
    fun usesTheSharedTypeClauseNode() {
        val tree = p.parse("json_value(d, '\$.a' returning number type(strict))")
        val clauses = tree.getDescendants(SingleRowSqlFunctionsGrammar.JSON_TYPE_CLAUSE)
        assertThatAst(clauses).hasSize(1)
        assertThatAst(clauses[0].children.map { it.tokenOriginalValue.lowercase() }).containsExactly("type", "(", "strict", ")")

        val nested = p.parse("json_query(json_transform(d, set '\$.a' = 1 type(lax)), '\$' type(strict))")
        assertThatAst(nested.getDescendants(SingleRowSqlFunctionsGrammar.JSON_TYPE_CLAUSE)).hasSize(2)
    }
}
