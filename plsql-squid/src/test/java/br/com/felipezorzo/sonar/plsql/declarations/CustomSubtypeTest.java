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
package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CustomSubtypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CUSTOM_SUBTYPE);
    }

    @Test
    public void matchesSimpleSubtype() {
        assertThat(p).matches("subtype sub is char;");
    }
    
    @Test
    public void matchesSubtypeWithScale() {
        assertThat(p).matches("subtype sub is number(5);");
    }
    
    @Test
    public void matchesSubtypeWithScaleAndPrecision() {
        assertThat(p).matches("subtype sub is number(5,1);");
    }
    
    @Test
    public void matchesSubtypeWithNotNullConstraint() {
        assertThat(p).matches("subtype sub is number not null;");
    }
    
    @Test
    public void matchesSubtypeWithRange() {
        assertThat(p).matches("subtype sub is number range 1..2;");
    }

}
