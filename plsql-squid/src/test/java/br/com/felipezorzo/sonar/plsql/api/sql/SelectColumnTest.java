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
package br.com.felipezorzo.sonar.plsql.api.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class SelectColumnTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SELECT_COLUMN);
    }
    
    @Test
    public void matchesAllColumns() {
        assertThat(p).matches("*");
    }
    
    @Test
    public void matchesFunctionCall() {
        assertThat(p).matches("func(var)");
    }
    
    @Test
    public void matchesCount() {
        assertThat(p).matches("count(*)");
    }
    
    @Test
    public void matchesColumnName() {
        assertThat(p).matches("name");
    }
    
    @Test
    public void matchesTableColumn() {
        assertThat(p).matches("tab.name");
    }
    
    @Test
    public void matchesAllTableColumns() {
        assertThat(p).matches("tab.*");
    }
    
    @Test
    public void matchesTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.name");
    }
    
    @Test
    public void matchesAllTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.*");
    }
    
    @Test
    public void matchesLiterals() {
        assertThat(p).matches("'x'");
        assertThat(p).matches("1");
        assertThat(p).matches("null");
    }
    
    @Test
    public void matchesSequenceCurrentValue() {
        assertThat(p).matches("seq.currval");
    }
    
    @Test
    public void matchesSequenceNextValue() {
        assertThat(p).matches("seq.nextval");
    }
    
    @Test
    public void matchesColumnWithAlias() {
        assertThat(p).matches("null alias");
    }
    
    @Test
    public void matchesColumnWithAliasWithAsKeyword() {
        assertThat(p).matches("null as alias");
    }
    
    @Test
    public void matchesSubqueryAsColumn() {
        assertThat(p).matches("(select 1 from dual) col");
    }

}
