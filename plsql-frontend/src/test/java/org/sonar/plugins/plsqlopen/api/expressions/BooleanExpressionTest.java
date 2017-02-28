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

public class BooleanExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleAndExpression() {
        assertThat(p).matches("true and true");
    }
    
    @Test
    public void matchesSimpleOrExpression() {
        assertThat(p).matches("true or true");
    }
    
    @Test
    public void matchesMultipleExpression() {
        assertThat(p).matches("true and true or true");
    }
    
    @Test
    public void matchesMultipleExpressionWithNotOperator() {
        assertThat(p).matches("true and not false");
    }
    
    @Test
    public void matchesExpressionWithVariables() {
        assertThat(p).matches("var and var");
    }
    
    @Test
    public void matchesExpressionWithFunctionCall() {
        assertThat(p).matches("func(var) and func(var)");
    }
    
}
