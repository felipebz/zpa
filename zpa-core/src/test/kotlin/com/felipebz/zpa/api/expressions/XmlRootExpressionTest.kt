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

class XmlRootExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlRoot() {
        assertThat(p).matches("xmlroot(xmltype('<foo/>'), version no value)")
    }

    @Test
    fun matchesXmlRootWithVersion() {
        assertThat(p).matches("xmlroot(xmltype('<foo/>'), version '1.0')")
    }

    @Test
    fun matchesXmlRootWithStandalone() {
        assertThat(p).matches("xmlroot(xmltype('<foo/>'), version '1.0', standalone no value)")
    }

}
