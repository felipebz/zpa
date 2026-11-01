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

class XmlExistsExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlExists() {
        assertThat(p).matches("xmlexists('/foo')")
    }

    @Test
    fun matchesXmlExistsWithPassingClause() {
        assertThat(p).matches("xmlexists('/foo' passing bar)")
    }

    @Test
    fun matchesXmlExistsWithPassingByValueClause() {
        assertThat(p).matches("xmlexists('/foo' passing by value bar)")
    }

    @Test
    fun matchesXmlExistsWithPassingMoreValues() {
        assertThat(p).matches("xmlexists('/foo' passing \"bar\" as bar, \"bar2\" as bar2)")
    }

}
