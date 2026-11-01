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

class XmlPiExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlPi() {
        assertThat(p).matches("xmlpi(name \"foo\")")
    }

    @Test
    fun matchesXmlPiWithEvalname() {
        assertThat(p).matches("xmlpi(evalname foo)")
    }

    @Test
    fun matchesXmlPiWithContent() {
        assertThat(p).matches("xmlpi(name \"foo\", 'bar')")
    }

}
