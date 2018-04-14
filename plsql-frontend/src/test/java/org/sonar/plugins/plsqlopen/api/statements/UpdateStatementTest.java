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
package org.sonar.plugins.plsqlopen.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class UpdateStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.UPDATE_STATEMENT);
    }
    
    @Test
    public void matchesSimpleUpdate() {
        assertThat(p).matches("update tab set x = 1;");
    }
    
    @Test
    public void matchesUpdateWithWhere() {
        assertThat(p).matches("update tab set x = 1 where y = 1;");
    }
    
    @Test
    public void matchesUpdateWithWhereCurrentOf() {
        assertThat(p).matches("update tab set x = 1 where current of cur;");
    }
    
    @Test
    public void matchesUpdateMultipleColumns() {
        assertThat(p).matches("update tab set x = 1, y = 1;");
    }
    
    @Test
    public void matchesUpdateWithAlias() {
        assertThat(p).matches("update tab t set t.x = 1;");
    }
    
    @Test
    public void matchesUpdateWithSchema() {
        assertThat(p).matches("update sch.tab set sch.tab.x = 1;");
    }
    
    @Test
    public void matchesLabeledUpdate() {
        assertThat(p).matches("<<foo>> update tab set x = 1;");
    }
    
    @Test
    public void matchesForallUpdate() {
        assertThat(p).matches("forall foo in 1 .. bar.count update tab set x = 1;");
    }
    
    @Test
    public void matchesUpdateWithReturningInto() {
        assertThat(p).matches("update tab set x = 1 returning x into y;");
    }
    
    @Test
    public void matchesUpdateDefaultValue() {
        assertThat(p).matches("update tab set x = default;");
    }
    
}
