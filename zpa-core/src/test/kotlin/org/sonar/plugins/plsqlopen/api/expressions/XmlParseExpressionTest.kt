/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
