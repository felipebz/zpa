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
package org.sonar.plugins.plsqlopen.api.ddl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateTableTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.CREATE_TABLE);
    }
    
    @Test
    public void matchesSimpleCreateTable() {
        assertThat(p).matches("create table tab (id number);");
    }
    
    @Test
    public void matchesMultipleColumns() {
        assertThat(p).matches("create table tab (id number, name number);");
    }
    
    @Test
    public void matchesTableWithSchema() {
        assertThat(p).matches("create table sch.tab (id number);");
    }
    
    @Test
    public void matchesTemporaryTable() {
        assertThat(p).matches("create global temporary table tab (id number);");
    }
    
    @Test
    public void matchesTemporaryTableWithTablespace() {
        assertThat(p).matches("create global temporary table tab (id number) tablespace table_space;");
    }
    
    @Test
    public void matchesTemporaryTableOnCommitDeleteRows() {
        assertThat(p).matches("create global temporary table tab (id number) on commit delete rows;");
    }
    
    @Test
    public void matchesTemporaryTableOnCommitPreserveRows() {
        assertThat(p).matches("create global temporary table tab (id number) on commit preserve rows;");
    }
    
    @Test
    public void matchesCreateTableWithOutOfLineConstraint() {
        assertThat(p).matches("create table tab (id number, constraint pk primary key(id));");
    }

}
