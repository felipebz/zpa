/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class XmlAttributesExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.XMLATTRIBUTES_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlAttributes() {
        assertThat(p).matches("xmlattributes(foo)");
    }
    
    @Test
    public void matchesXmlAttributesEntityEscaping() {
        assertThat(p).matches("xmlattributes(entityescaping foo)");
    }
    
    @Test
    public void matchesXmlAttributesNoEntityEscaping() {
        assertThat(p).matches("xmlattributes(noentityescaping foo)");
    }
    
    @Test
    public void matchesXmlAttributesSchemaCheck() {
        assertThat(p).matches("xmlattributes(schemacheck foo)");
    }
    
    @Test
    public void matchesXmlAttributesNoSchemaCheck() {
        assertThat(p).matches("xmlattributes(noschemacheck foo)");
    }
    
    @Test
    public void matchesXmlAttributesWithExpression() {
        assertThat(p).matches("xmlattributes(foo as \"bar\")");
    }
    
    @Test
    public void matchesXmlAttributesWithEvalNameExpression() {
        assertThat(p).matches("xmlattributes(foo as evalname bar)");
    }
    
    @Test
    public void matchesXmlAttributesWithMultipleAttributes() {
        assertThat(p).matches("xmlattributes(foo as \"bar\", foo2 as \"bar2\")");
    }

}
