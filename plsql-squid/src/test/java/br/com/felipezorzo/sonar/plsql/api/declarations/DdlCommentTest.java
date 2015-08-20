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
package br.com.felipezorzo.sonar.plsql.api.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class DdlCommentTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.DDL_COMMENT);
    }
    
    @Test
    public void matchesCommentOnTableWithSemicolon() {
        assertThat(p).matches("comment on table tab is 'test';");
    }
    
    @Test
    public void matchesCommentOnTable() {
        assertThat(p).matches("comment on table tab is 'test'");
    }
    
    @Test
    public void matchesCommentOnTableWithSchema() {
        assertThat(p).matches("comment on table sch.tab is 'test'");
    }
    
    @Test
    public void matchesCommentOnColumn() {
        assertThat(p).matches("comment on column tab.col is 'test'");
    }
    
    @Test
    public void matchesCommentOnColumnWithSchema() {
        assertThat(p).matches("comment on column sch.tab.col is 'test'");
    }
    
    @Test
    public void matchesCommentOnOperator() {
        assertThat(p).matches("comment on operator foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnOperatorWithSchema() {
        assertThat(p).matches("comment on operator sch.foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnIndextype() {
        assertThat(p).matches("comment on indextype foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnIndextypeWithSchema() {
        assertThat(p).matches("comment on indextype sch.foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnMaterializedView() {
        assertThat(p).matches("comment on materialized view foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnMaterializedViewWithSchema() {
        assertThat(p).matches("comment on materialized view sch.foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnMiningModelView() {
        assertThat(p).matches("comment on materialized view foo is 'test'");
    }
    
    @Test
    public void matchesCommentOnMiningModelWithSchema() {
        assertThat(p).matches("comment on materialized view sch.foo is 'test'");
    }
    
}
