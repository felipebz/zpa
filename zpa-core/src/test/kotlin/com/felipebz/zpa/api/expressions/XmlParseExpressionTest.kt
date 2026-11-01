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

class XmlParseExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlParse() {
        assertThat(p).matches("xmlparse(content '<foo>bar</bar>')")
    }

    @Test
    fun matchesXmlParseDocument() {
        assertThat(p).matches("xmlparse(document '<foo>bar</bar>')")
    }

    @Test
    fun matchesXmlParseDocumentWellformed() {
        assertThat(p).matches("xmlparse(document '<foo>bar</bar>' wellformed)")
    }

}
