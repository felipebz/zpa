/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class XmlTableExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlTable() {
        assertThat(p).matches("xmltable('/foo' passing bar)");
    }
    
    @Test
    public void matchesXmlTableWithNamespaces() {
        assertThat(p).matches("xmltable(xmlnamespaces ('foo' as bar, default 'foo2'), '/foo/bar:foo' passing bar)");
    }
    
    @Test
    public void matchesXmlTableReturningSequenceByRef() {
        assertThat(p).matches("xmltable('/foo' passing bar returning sequence by ref)");
    }    
    @Test
    public void matchesXmlTableWithColumns() {
        assertThat(p).matches("xmltable('/foo' passing bar columns \"foo\" varchar2(1) path 'bar' default 'a', \"foo2\" varchar2(1) path 'bar2', \"foo3\" for ordinality)");
    }
    
}
