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

class XmlColattvalExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlCollattval() {
        assertThat(p).matches("xmlcolattval(foo, bar, foo2, bar2)")
    }

    @Test
    fun matchesXmlColattvalWithExpressionWithoutAs() {
        assertThat(p).matches("xmlcolattval(foo \"bar\")")
    }

    @Test
    fun matchesXmlColattvalWithEvalname() {
        assertThat(p).matches("xmlcolattval(foo as bar, foo as evalname bar)")
    }
}
