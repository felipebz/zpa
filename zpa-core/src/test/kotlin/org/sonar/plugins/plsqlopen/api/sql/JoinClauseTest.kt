/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.sql

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class JoinClauseTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(DmlGrammar.JOIN_CLAUSE)
    }

    @Test
    fun matchesSimpleJoin() {
        assertThat(p).matches("foo join bar on foo.a = bar.a")
    }

    @Test
    fun matchesSimpleJoinWithTableAlias() {
        assertThat(p).matches("foo f join bar b on f.a = b.a")
    }

    @Test
    fun matchesInnerJoin() {
        assertThat(p).matches("foo inner join bar on foo.a = bar.a")
    }

    @Test
    fun matchesInnerJoinWithTableAlias() {
        assertThat(p).matches("foo f inner join bar b on f.a = b.a")
    }

    @Test
    fun matchesJoinWithUsing() {
        assertThat(p).matches("foo join bar using (a)")
    }

    @Test
    fun matchesJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f join bar b using (a)")
    }


    @Test
    fun matchesInnerJoinWithUsing() {
        assertThat(p).matches("foo inner join bar using (a)")
    }

    @Test
    fun matchesInnerJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f inner join bar n using (a)")
    }

    @Test
    fun matchesJoinWithUsingAndMultipleColumns() {
        assertThat(p).matches("foo join bar using (a, b, c)")
    }

    @Test
    fun matchesCrossJoin() {
        assertThat(p).matches("foo cross join bar")
    }

    @Test
    fun matchesCrossJoinWithTableAlias() {
        assertThat(p).matches("foo f cross join ba br")
    }

    @Test
    fun matchesNaturalJoin() {
        assertThat(p).matches("foo natural join bar")
    }

    @Test
    fun matchesNaturalJoinWithTableAlias() {
        assertThat(p).matches("foo f natural join bar b")
    }

    @Test
    fun matchesNaturalInnerJoin() {
        assertThat(p).matches("foo natural inner join bar")
    }

    @Test
    fun matchesFullJoin() {
        assertThat(p).matches("foo full join bar on foo.a = bar.a")
    }

    @Test
    fun matchesFullJoinWithTableAlias() {
        assertThat(p).matches("foo f full join bar b on f.a = b.a")
    }

    @Test
    fun matchesFullJoinWithUsing() {
        assertThat(p).matches("foo full join bar using (a)")
    }

    @Test
    fun matchesFullJoinWithUsingWithTableAlias() {
        assertThat(p).matches("foo f full join bar b using (a)")
    }

    @Test
    fun matchesFullJoinWithUsingMultipleColumns() {
        assertThat(p).matches("foo full join bar using (a, b, c)")
    }

    @Test
    fun matchesFullOuterJoin() {
        assertThat(p).matches("foo full outer join bar on foo.a = bar.a")
    }

    @Test
    fun matchesRightJoin() {
        assertThat(p).matches("foo right join bar on foo.a = bar.a")
    }

    @Test
    fun matchesRightJoinWithTableAlias() {
        assertThat(p).matches("foo f right join bar b on f.a = b.a")
    }

    @Test
    fun matchesRightOuterJoin() {
        assertThat(p).matches("foo right outer join bar on foo.a = bar.a")
    }

    @Test
    fun matchesLeftJoin() {
        assertThat(p).matches("foo left join bar on foo.a = bar.a")
    }

    @Test
    fun matchesLeftJoinWithTableAlias() {
        assertThat(p).matches("foo f left join bar b on f.a = b.a")
    }

    @Test
    fun matchesLeftOuterJoin() {
        assertThat(p).matches("foo left outer join bar on foo.a = bar.a")
    }

    @Test
    fun matchesNaturalFullJoin() {
        assertThat(p).matches("foo natural full join bar")
    }

    @Test
    fun matchesNaturalFullOuterJoin() {
        assertThat(p).matches("foo natural full outer join bar")
    }

    @Test
    fun matchesNaturalRightJoin() {
        assertThat(p).matches("foo natural right join bar")
    }

    @Test
    fun matchesNaturalRightOuterJoin() {
        assertThat(p).matches("foo natural right outer join bar")
    }

    @Test
    fun matchesNaturalLeftJoin() {
        assertThat(p).matches("foo natural left join bar")
    }

    @Test
    fun matchesNaturalLeftOuterJoin() {
        assertThat(p).matches("foo natural left outer join bar")
    }

    @Test
    fun matchesOuterJoinWithQueryPartition() {
        assertThat(p).matches("foo partition by a, b left join bar on foo.a = bar.a")
        assertThat(p).matches("foo partition by (a, b) left join bar on foo.a = bar.a")

        assertThat(p).matches("foo left join bar partition by a, b on foo.a = bar.a")
        assertThat(p).matches("foo left join bar partition by (a, b) on foo.a = bar.a")
    }

    @Test
    fun matchesJoinSubqueryWithTable() {
        assertThat(p).matches("(select a from foo) f join bar b on f.a = b.a")
    }

    @Test
    fun matchesJoinTableWithSubquery() {
        assertThat(p).matches("foo join (select a from bar) b on foo.a = b.a")
    }


}
