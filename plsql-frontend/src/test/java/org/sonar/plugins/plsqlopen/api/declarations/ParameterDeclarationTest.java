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
package org.sonar.plugins.plsqlopen.api.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class ParameterDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.PARAMETER_DECLARATION);
    }

    @Test
    public void matchesSimpleParameter() {
        assertThat(p).matches("parameter number");
    }
    
    @Test
    public void matchesExplicitInParameter() {
        assertThat(p).matches("parameter in number");
    }
    
    @Test
    public void matchesExplicitInParameterWithDefaultValue() {
        assertThat(p).matches("parameter in number := 1");
    }
    
    @Test
    public void matchesExplicitInParameterWithDefaultValueAlternative() {
        assertThat(p).matches("parameter in number default 1");
    }
    
    @Test
    public void matchesOutParameter() {
        assertThat(p).matches("parameter out number");
    }
    
    @Test
    public void matchesInOutParameter() {
        assertThat(p).matches("parameter in out number");
    }
    
    @Test
    public void matchesOutParameterWithNocopy() {
        assertThat(p).matches("parameter out nocopy number");
    }
    
    @Test
    public void matchesInOutParameterWithNocopy() {
        assertThat(p).matches("parameter in out nocopy number");
    }
    
}
