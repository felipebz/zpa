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

class XmlForestExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlForest() {
        assertThat(p).matches("xmlforest(foo)")
    }

    @Test
    fun matchesXmlForestWithExpressionWithoutAs() {
        assertThat(p).matches("xmlforest(foo \"bar\")")
    }

    @Test
    fun matchesXmlForestWithExpression() {
        assertThat(p).matches("xmlforest(foo as \"bar\")")
    }

    @Test
    fun matchesXmlForestWithEvalNameExpression() {
        assertThat(p).matches("xmlforest(foo as evalname bar)")
    }

    @Test
    fun matchesXmlForestWithMultipleAttributes() {
        assertThat(p).matches("xmlforest(foo as \"bar\", foo2 as \"bar2\")")
    }

}
