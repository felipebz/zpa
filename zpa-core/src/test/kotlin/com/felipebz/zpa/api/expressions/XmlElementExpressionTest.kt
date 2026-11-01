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

class XmlElementExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlElement() {
        assertThat(p).matches("xmlelement(\"xml\")")
    }

    @Test
    fun matchesXmlElementEntityEscaping() {
        assertThat(p).matches("xmlelement(entityescaping \"xml\")")
    }

    @Test
    fun matchesXmlElementEntityNoEscaping() {
        assertThat(p).matches("xmlelement(noentityescaping \"xml\")")
    }

    @Test
    fun matchesXmlElementExplicitNameKeyword() {
        assertThat(p).matches("xmlelement(name \"xml\")")
    }

    @Test
    fun matchesXmlElementEvalNameKeyword() {
        assertThat(p).matches("xmlelement(evalname \"xml\")")
    }

    @Test
    fun matchesXmlElementWithXmlAttributes() {
        assertThat(p).matches("xmlelement(\"xml\", xmlattributes(foo as \"bar\"))")
    }

    @Test
    fun matchesXmlElementWithValue() {
        assertThat(p).matches("xmlelement(\"xml\", foo)")
    }

    @Test
    fun matchesXmlElementWithValueAndAlias() {
        assertThat(p).matches("xmlelement(\"xml\", foo as bar)")
        assertThat(p).matches("xmlelement(\"xml\", foo bar)")
    }

    @Test
    fun matchesXmlElementWithMultipleValues() {
        assertThat(p).matches("xmlelement(\"xml\", foo, bar, baz)")
    }

}
