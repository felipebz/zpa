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
        assertThat(p).matches("json_query(foo, '$' type (strict))")
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
            type (lax))""")
    }

}
