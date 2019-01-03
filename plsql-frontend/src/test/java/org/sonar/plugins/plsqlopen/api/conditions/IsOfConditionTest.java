/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.conditions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.ConditionsGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class IsOfConditionTest extends RuleTest {
    
    @Before
    public void init() {
        setRootRule(ConditionsGrammar.CONDITION);
    }
    
    @Test
    public void matchesSimpleIsOf() {
        assertThat(p).matches("foo is of (bar)");
    }

    @Test
    public void matchesSimpleIsNotOf() {
        assertThat(p).matches("foo is not of (bar)");
    }

    @Test
    public void matchesIsOfType() {
        assertThat(p).matches("foo is of type (bar)");
    }

    @Test
    public void matchesIsNotOfType() {
        assertThat(p).matches("foo is not of type (bar)");
    }

    @Test
    public void matchesIsOfMultipleType() {
        assertThat(p).matches("foo is of type (bar, baz, foobar)");
    }

    @Test
    public void matchesIsOfOnly() {
        assertThat(p).matches("foo is of type (only bar)");
    }

}
