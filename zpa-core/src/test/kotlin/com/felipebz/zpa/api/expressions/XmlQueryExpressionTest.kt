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

class XmlQueryExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlQuery() {
        assertThat(p).matches("xmlquery('/foo' returning content)")
    }

    @Test
    fun matchesXmlQueryWithNullOnEmpty() {
        assertThat(p).matches("xmlquery('/foo' returning content null on empty)")
    }

    @Test
    fun matchesXmlQueryWithPassingByValueClause() {
        assertThat(p).matches("xmlquery('/foo' passing by value bar returning content)")
    }

    @Test
    fun matchesXmlQueryWithPassingMoreValues() {
        assertThat(p).matches("xmlquery('/foo' passing \"bar\" as bar, \"bar2\" as bar2 returning content)")
    }


    @Test
    fun matchesXmlQueryWithQueryExpression() {
        assertThat(p).matches("xmlquery(l_query passing doc returning content)")
        assertThat(p).matches("xmlquery('/a' || '/b' passing doc returning content)")
    }

    @Test
    fun doesNotMatchXmlQueryWithoutReturningContent() {
        assertThat(p).notMatches("xmlquery(l_query passing doc)")
    }
}
