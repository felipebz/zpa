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

public class InsertStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.INSERT_STATEMENT);
    }
    
    @Test
    public void matchesSimpleInsert() {
        assertThat(p).matches("insert into tab values (1);");
    }
    
    @Test
    public void matchesInsertWithTableAlias() {
        assertThat(p).matches("insert into tab t values (1);");
    }
    
    @Test
    public void matchesInsertWithExplicitColumn() {
        assertThat(p).matches("insert into tab (x) values (1);");
    }
    
    @Test
    public void matchesInsertWithExplicitColumnAlternative() {
        assertThat(p).matches("insert into tab (tab.x) values (1);");
    }
    
    @Test
    public void matchesInsertMultipleColumns() {
        assertThat(p).matches("insert into tab (x, y) values (1, 2);");
    }
    
    @Test
    public void matchesInsertWithSubquery() {
        assertThat(p).matches("insert into tab (select 1, 2 from dual);");
    }
    
    @Test
    public void matchesInsertWithSubqueryInColumns() {
        assertThat(p).matches("insert into tab (x, y) (select 1, 2 from dual);");
    }
    
    @Test
    public void matchesInsertWithSchema() {
        assertThat(p).matches("insert into sch.tab values (1);");
    }
    
    @Test
    public void matchesInsertRecord() {
        assertThat(p).matches("insert into tab values foo;");
    }
    
    @Test
    public void matchesLabeledInsert() {
        assertThat(p).matches("<<foo>> insert into tab values (1);");
    }
    
    @Test
    public void matchesForallInsert() {
        assertThat(p).matches("forall x in values of bar insert into tab values (1);");
    }
    
    @Test
    public void matchesInsertWithReturningInto() {
        assertThat(p).matches("insert into tab (x) values (1) returning x into y;");
    }

}
