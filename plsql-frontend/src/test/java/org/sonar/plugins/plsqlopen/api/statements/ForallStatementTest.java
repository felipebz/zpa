/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

public class ForallStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FORALL_STATEMENT);
    }
    
    @Test
    public void matchesForallWithFixedRange() {
        assertThat(p).matches("forall foo in 1 .. 2");
    }
    
   @Test
    public void matchesForallWithVariablesRange() {
        assertThat(p).matches("forall x in foo .. bar.count");
    }
    
    @Test
    public void matchesForallIndicesOf() {
        assertThat(p).matches("forall foo in indices of bar");
    }
    
    @Test
    public void matchesForallIndicesOfWithRange() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2");
    }
    
    @Test
    public void matchesForallValuesOf() {
        assertThat(p).matches("forall foo in values of bar");
    }
    
    @Test
    public void matchesForallValuesWithSaveExceptions() {
        assertThat(p).matches("forall foo in values of bar save exceptions");
    }
    
    @Test
    public void matchesForallIndicesWithSaveExceptions() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2 save exceptions");
    }

}
