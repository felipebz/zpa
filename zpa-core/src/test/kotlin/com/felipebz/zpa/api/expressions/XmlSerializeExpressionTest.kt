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

class XmlSerializeExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlSerializeDocument() {
        assertThat(p).matches("xmlserialize(document 'foo')")
    }

    @Test
    fun matchesSimpleXmlSerializeContent() {
        assertThat(p).matches("xmlserialize(content 'foo')")
    }

    @Test
    fun matchesXmlSerializeAsDatatype() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob)")
    }

    @Test
    fun matchesXmlSerializeWithEncoding() {
        assertThat(p).matches("xmlserialize(document 'foo' encoding 'utf-8')")
    }

    @Test
    fun matchesXmlSerializeWithVersion() {
        assertThat(p).matches("xmlserialize(document 'foo' version '1.0')")
    }

    @Test
    fun matchesXmlSerializeWithNoIdent() {
        assertThat(p).matches("xmlserialize(document 'foo' no indent)")
    }

    @Test
    fun matchesXmlSerializeWithIndent() {
        assertThat(p).matches("xmlserialize(document 'foo' indent)")
    }

    @Test
    fun matchesXmlSerializeWithIndentAndSize() {
        assertThat(p).matches("xmlserialize(document 'foo' indent size = 1)")
    }

    @Test
    fun matchesXmlSerializeWithHideDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' hide defaults)")
    }

    @Test
    fun matchesXmlSerializeWithShowDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' show defaults)")
    }

    @Test
    fun matchesLongXmlSerializeExpression() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob encoding 'utf-8' version '1.0' indent size = 1 show defaults)")
    }

}
