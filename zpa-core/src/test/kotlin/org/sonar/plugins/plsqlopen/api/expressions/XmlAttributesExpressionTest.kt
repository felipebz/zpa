/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.expressions

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

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
