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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class JsonQueryExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleJsonQuery() {
        assertThat(p).matches("json_query(foo, '$')")
    }

    @Test
    fun matchesJsonQuery() {
        assertThat(p).matches("json_query(foo format json, '$')")
    }

    @Test
    fun matchesJsonQueryWithType() {
        assertThat(p).matches("json_query(foo, '$' type strict)")
    }

    @Test
    fun matchesJsonQueryWithPassing() {
        assertThat(p).matches("json_query(foo, '$' passing 'x' as bar)")
    }

    @Test
    fun matchesJsonQueryWithNullOnEmpty() {
        assertThat(p).matches("json_query(foo, '$' null on empty)")
    }

    @Test
    fun matchesJsonQueryWithNullOnError() {
        assertThat(p).matches("json_query(foo, '$' null on error)")
    }

    @Test
    fun matchesJsonQueryWithNullOnMismatch() {
        assertThat(p).matches("json_query(foo, '$' null on mismatch)")
    }

    @Test
    fun matchesJsonQueryWithReturning() {
        assertThat(p).matches("json_query(foo, '$' returning json)")
    }

    @Test
    fun matchesJsonQueryWithPassingMoreValues() {
        assertThat(p).matches("json_query(foo, '$' passing 1 as bar, 2 as bar2 returning json)")
    }

    @Test
    fun matchesJsonQueryWithWrapper() {
        assertThat(p).matches("json_query(foo, '$' with array wrapper)")
    }

    @Test
    fun matchesJsonQueryWithQuotesClause() {
        assertThat(p).matches("json_query(foo, '$' keep quotes)")
    }

    @Test
    fun matchesLongJsonQuery() {
        assertThat(p).matches("""json_query(foo, '$' 
            passing 1 as bar, 2 as baz 
            returning json allow scalars pretty ascii 
            with conditional array wrapper 
            keep quotes on scalar string 
            empty object on error 
            empty array on empty 
            error on mismatch 
            type lax)""")
    }

}
