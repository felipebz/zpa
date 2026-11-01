//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class XmlAttributesExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlAttributes() {
        assertThat(p).matches("xmlattributes(foo)")
    }

    @Test
    fun matchesXmlAttributesEntityEscaping() {
        assertThat(p).matches("xmlattributes(entityescaping foo)")
    }

    @Test
    fun matchesXmlAttributesNoEntityEscaping() {
        assertThat(p).matches("xmlattributes(noentityescaping foo)")
    }

    @Test
    fun matchesXmlAttributesSchemaCheck() {
        assertThat(p).matches("xmlattributes(schemacheck foo)")
    }

    @Test
    fun matchesXmlAttributesNoSchemaCheck() {
        assertThat(p).matches("xmlattributes(noschemacheck foo)")
    }

    @Test
    fun matchesXmlAttributesWithExpressionWithoutAs() {
        assertThat(p).matches("xmlattributes(foo \"bar\")")
    }

    @Test
    fun matchesXmlAttributesWithExpression() {
        assertThat(p).matches("xmlattributes(foo as \"bar\")")
    }

    @Test
    fun matchesXmlAttributesWithEvalNameExpression() {
        assertThat(p).matches("xmlattributes(foo as evalname bar)")
    }

    @Test
    fun matchesXmlAttributesWithMultipleAttributes() {
        assertThat(p).matches("xmlattributes(foo as \"bar\", foo2 as \"bar2\")")
    }

}
