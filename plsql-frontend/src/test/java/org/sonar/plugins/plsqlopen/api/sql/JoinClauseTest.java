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

public class JoinClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DmlGrammar.JOIN_CLAUSE);
    }

    @Test
    public void matchesSimpleJoin() {
        assertThat(p).matches("foo join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesSimpleJoinWithTableAlias() {
        assertThat(p).matches("foo f join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesInnerJoin() {
        assertThat(p).matches("foo inner join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesInnerJoinWithTableAlias() {
        assertThat(p).matches("foo f inner join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesJoinWithUsing() {
        assertThat(p).matches("foo join bar using (a)");
    }
    
    @Test
    public void matchesJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f join bar b using (a)");
    }
    
    
    @Test
    public void matchesInnerJoinWithUsing() {
        assertThat(p).matches("foo inner join bar using (a)");
    }
    
    @Test
    public void matchesInnerJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f inner join bar n using (a)");
    }
    
    @Test
    public void matchesJoinWithUsingAndMultipleColumns() {
        assertThat(p).matches("foo join bar using (a, b, c)");
    }
    
    @Test
    public void matchesCrossJoin() {
        assertThat(p).matches("foo cross join bar");
    }
    
    @Test
    public void matchesCrossJoinWithTableAlias() {
        assertThat(p).matches("foo f cross join ba br");
    }
    
    @Test
    public void matchesNaturalJoin() {
        assertThat(p).matches("foo natural join bar");
    }
    
    @Test
    public void matchesNaturalJoinWithTableAlias() {
        assertThat(p).matches("foo f natural join bar b");
    }
    
    @Test
    public void matchesNaturalInnerJoin() {
        assertThat(p).matches("foo natural inner join bar");
    }
    
    @Test
    public void matchesFullJoin() {
        assertThat(p).matches("foo full join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesFullJoinWithTableAlias() {
        assertThat(p).matches("foo f full join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesFullJoinWithUsing() {
        assertThat(p).matches("foo full join bar using (a)");
    }
    
    @Test
    public void matchesFullJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f full join bar b using (a)");
    }
    
    @Test
    public void matchesFullJoinWithUsingMultipleColumns() {
        assertThat(p).matches("foo full join bar using (a, b, c)");
    }
    
    @Test
    public void matchesFullOuterJoin() {
        assertThat(p).matches("foo full outer join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesRightJoin() {
        assertThat(p).matches("foo right join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesRightJoinWithTableAlias() {
        assertThat(p).matches("foo f right join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesRightOuterJoin() {
        assertThat(p).matches("foo right outer join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesLeftJoin() {
        assertThat(p).matches("foo left join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesLeftJoinWithTableAlias() {
        assertThat(p).matches("foo f left join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesLeftOuterJoin() {
        assertThat(p).matches("foo left outer join bar on foo.a = bar.a");
    }
    
    @Test
    public void matchesNaturalFullJoin() {
        assertThat(p).matches("foo natural full join bar");
    }
    
    @Test
    public void matchesNaturalFullOuterJoin() {
        assertThat(p).matches("foo natural full outer join bar");
    }
    
    @Test
    public void matchesNaturalRightJoin() {
        assertThat(p).matches("foo natural right join bar");
    }
    
    @Test
    public void matchesNaturalRightOuterJoin() {
        assertThat(p).matches("foo natural right outer join bar");
    }
    
    @Test
    public void matchesNaturalLeftJoin() {
        assertThat(p).matches("foo natural left join bar");
    }
    
    @Test
    public void matchesNaturalLeftOuterJoin() {
        assertThat(p).matches("foo natural left outer join bar");
    }
    
    @Test
    public void matchesOuterJoinWithQueryPartition() {
        assertThat(p).matches("foo partition by a, b left join bar on foo.a = bar.a");
        assertThat(p).matches("foo partition by (a, b) left join bar on foo.a = bar.a");
        
        assertThat(p).matches("foo left join bar partition by a, b on foo.a = bar.a");
        assertThat(p).matches("foo left join bar partition by (a, b) on foo.a = bar.a");
    }
    
    @Test
    public void matchesJoinSubqueryWithTable() {
        assertThat(p).matches("(select a from foo) f join bar b on f.a = b.a");
    }
    
    @Test
    public void matchesJoinTableWithSubquery() {
        assertThat(p).matches("foo join (select a from bar) b on foo.a = b.a");
    }
    
    
}
