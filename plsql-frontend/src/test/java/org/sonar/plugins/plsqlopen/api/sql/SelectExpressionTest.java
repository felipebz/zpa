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

public class SelectExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleSelect() {
        assertThat(p).matches("select 1 from dual");
    }
    
    @Test
    public void matchesSimpleSelectInto() {
        assertThat(p).matches("select 1 into var from dual");
    }
    
    @Test
    public void matchesSelectBulkCollectInto() {
        assertThat(p).matches("select 1 bulk collect into var from dual");
    }
    
    @Test
    public void matchesSelectWithWhere() {
        assertThat(p).matches("select 1 from dual where 1 = 1");
    }
    
    @Test
    public void matchesSelectWithMultipleColumns() {
        assertThat(p).matches("select 1, 2 from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleColumnsAndIntoClause() {
        assertThat(p).matches("select 1, 2 into var1, var2 from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleTables() {
        assertThat(p).matches("select 1 from emp, dept");
    }
    
    @Test
    public void matchesSelectAll() {
        assertThat(p).matches("select all 1 from dual");
    }
    
    @Test
    public void matchesSelectDistinct() {
        assertThat(p).matches("select distinct 1 from dual");
    }
    
    @Test
    public void matchesSelectUnique() {
        assertThat(p).matches("select unique 1 from dual");
    }
    
    @Test
    public void matchesSelectWithGroupBy() {
        assertThat(p).matches("select 1 from dual group by 1");
    }
    
    @Test
    public void matchesSelectWithOrderBy() {
        assertThat(p).matches("select 1 from dual order by 1");
    }

    @Test
    public void matchesSelectWithParenthesis() {
        assertThat(p).matches("(select 1 from dual)");
    }
    
    @Test
    public void matchesSelectWithUnion() {
        assertThat(p).matches("select 1 from dual union select 2 from dual");
        assertThat(p).matches("(select 1 from dual) union (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectWithUnionAll() {
        assertThat(p).matches("select 1 from dual union all select 2 from dual");
        assertThat(p).matches("(select 1 from dual) union all (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectWithMinus() {
        assertThat(p).matches("(select 1 from dual) minus (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectWithIntersect() {
        assertThat(p).matches("(select 1 from dual) intersect (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectCountDistinct() {
        assertThat(p).matches("select count(distinct foo) from dual");
    }
    
    @Test
    public void matchesSelectWithAnalyticFunction() {
        assertThat(p).matches("select count(foo) over () from dual");
        assertThat(p).matches("select (count(foo) over ()) from dual");
        assertThat(p).matches("select func(count(foo) over ()) from dual");
    }
    
    @Test
    public void matchesSelectWithAnsiJoin() {
        assertThat(p).matches("select 1 from foo join bar on join.id = bar.id");
    }
    
    @Test
    public void matchesSelectWithMixedJoinSyntax() {
        assertThat(p).matches("select 1 from foo join bar on join.id = bar.id, baz");
        assertThat(p).matches("select 1 from baz, foo join bar on join.id = bar.id");
    }
    
    @Test
    public void matchesSelectWithSubqueryFactoring() {
        assertThat(p).matches("with foo as (select id from tab) select 1 from foo join bar on join.id = bar.id");
    }

    @Test
    public void matchesSelectForUpdateBeforeOrderBy() {
        assertThat(p).matches("select * from foo for update order by bar ");
    }
    
    @Test
    public void matchesSelectForUpdateAfterOrderBy() {
        assertThat(p).matches("select * from foo order by baz for update");
    }
    
    @Test
    public void matchesSelectWithParenthesisForUpdate() {
        assertThat(p).matches("(select 1 from dual) for update");
    }
    
    @Test
    public void matchesSelectUsingBulkAsAnAlias() {
        assertThat(p).matches("select 1 bulk into var from dual");
    }
    
    @Test
    public void matchesSelectBulkCollectUsingBulkAsAnAlias() {
        assertThat(p).matches("select 1 bulk bulk collect into var from dual");
    }
    
    @Test
    public void matchesSelectWithFetchFirstRowsOnly() {
        assertThat(p).matches("select 1 from dual fetch first 1 row only");
    }    
    
    @Test
    public void matchesSelectWithOrderByAndFetchFirstRows() {
        assertThat(p).matches("select 1 from dual order by 1 fetch first 1 row only");
    }    
}
