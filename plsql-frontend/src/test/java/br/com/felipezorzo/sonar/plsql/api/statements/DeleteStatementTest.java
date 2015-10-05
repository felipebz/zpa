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
package br.com.felipezorzo.sonar.plsql.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class DeleteStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.DELETE_STATEMENT);
    }

    @Test
    public void matchesSimpleDelete() {
        assertThat(p).matches("delete tab;");
    }
    
    @Test
    public void matchesDeleteFrom() {
        assertThat(p).matches("delete from tab;");
    }
    
    @Test
    public void matchesDeleteWithWhere() {
        assertThat(p).matches("delete from tab where x = 1;");
    }
    
    @Test
    public void matchesDeleteWithAlias() {
        assertThat(p).matches("delete tab t;");
    }
    
    @Test
    public void matchesDeleteWithSchema() {
        assertThat(p).matches("delete sch.tab;");
    }
    
    @Test
    public void matchesDeleteCurrentOf() {
        assertThat(p).matches("delete tab where current of cur;");
    }
    
    @Test
    public void matchesLabeledDelete() {
        assertThat(p).matches("<<foo>> delete tab;");
    }

}
