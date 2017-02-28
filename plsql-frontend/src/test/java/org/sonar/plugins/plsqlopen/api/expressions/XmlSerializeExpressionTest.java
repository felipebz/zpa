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

public class XmlSerializeExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlSerializeDocument() {
        assertThat(p).matches("xmlserialize(document 'foo')");
    }
    
    @Test
    public void matchesSimpleXmlSerializeContent() {
        assertThat(p).matches("xmlserialize(content 'foo')");
    }
    
    @Test
    public void matchesXmlSerializeAsDatatype() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob)");
    }
    
    @Test
    public void matchesXmlSerializeWithEncoding() {
        assertThat(p).matches("xmlserialize(document 'foo' encoding 'utf-8')");
    }
    
    @Test
    public void matchesXmlSerializeWithVersion() {
        assertThat(p).matches("xmlserialize(document 'foo' version '1.0')");
    }
    
    @Test
    public void matchesXmlSerializeWithNoIdent() {
        assertThat(p).matches("xmlserialize(document 'foo' no ident)");
    }
    
    @Test
    public void matchesXmlSerializeWithIdent() {
        assertThat(p).matches("xmlserialize(document 'foo' ident)");
    }
    
    @Test
    public void matchesXmlSerializeWithIdentAndSize() {
        assertThat(p).matches("xmlserialize(document 'foo' ident size = 1)");
    }
    
    @Test
    public void matchesXmlSerializeWithHideDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' hide defaults)");
    }
    
    @Test
    public void matchesXmlSerializeWithShowDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' show defaults)");
    }
    
    @Test
    public void matchesLongXmlSerializeExpression() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob encoding 'utf-8' version '1.0' ident size = 1 show defaults)");
    }

}
