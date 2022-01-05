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

class XmlTableExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlTable() {
        assertThat(p).matches("xmltable('/foo' passing bar)")
        assertThat(p).matches("xmltable('/foo' passing bar.baz)")
    }

    @Test
    fun matchesXmlTableWithNamespaces() {
        assertThat(p).matches("xmltable(xmlnamespaces ('foo' as bar, default 'foo2'), '/foo/bar:foo' passing bar)")
    }

    @Test
    fun matchesXmlTableReturningSequenceByRef() {
        assertThat(p).matches("xmltable('/foo' passing bar returning sequence by ref)")
    }

    @Test
    fun matchesXmlTableWithColumns() {
        assertThat(p).matches("xmltable('/foo' passing bar columns \"foo\" varchar2(1) path 'bar' default 'a', \"foo2\" varchar2(1) path 'bar2', \"foo3\" for ordinality)")
    }

}
