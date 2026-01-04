/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

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
