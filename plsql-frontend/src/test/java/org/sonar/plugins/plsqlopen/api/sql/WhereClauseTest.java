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
package org.sonar.plugins.plsqlopen.api.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class WhereClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DmlGrammar.WHERE_CLAUSE);
    }
    
    @Test
    public void matchesSimpleWhere() {
        assertThat(p).matches("where 1 = 1");
    }
    
    @Test
    public void matchesColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2");
    }
    
    @Test
    public void matchesMultipleColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2 and tab.col = tab2.col2");
    }
    
    @Test
    public void matchesComparationWithSubquery() {
        assertThat(p).matches("where tab.col = (select 1 from dual)");
    }
    
    @Test
    public void matchesOuterJoin() {
        assertThat(p).matches("where tab.col(+) = tab2.col2");
    }
    
    @Test
    public void matchesTupleInSelect() {
        assertThat(p).matches("where (foo, bar, baz) in (select 1, 2, 3 from dual)");
    }
    
    @Test
    public void matchesTupleInValues() {
        assertThat(p).matches("where (foo, bar) in ((1, 2), (2, 3))");
    }
    
    @Test
    public void matchesExists() {
        assertThat(p).matches("where exists (select 1 from dual)");
    }
    
    @Test
    public void matchesNotExists() {
        assertThat(p).matches("where not exists (select 1 from dual)");
    }
    
    @Test
    public void matchesAny() {
    	assertThat(p).matches("where data = any (select 1,2,3 from dual)");
    }
}
