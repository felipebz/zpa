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
    
    @Test
    public void matchesDeleteFromQuery() {
        assertThat(p).matches("delete (select * from dual);");
    }

    @Test
    public void matchesForallDelete() {
        assertThat(p).matches("forall x in indices of bar delete tab;");
    }
    
    @Test
    public void matchesDeleteWithReturningInto() {
        assertThat(p).matches("delete from tab returning x bulk collect into y;");
    }
}
