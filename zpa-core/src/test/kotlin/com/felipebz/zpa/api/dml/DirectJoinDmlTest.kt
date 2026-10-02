/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.api.dml

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class DirectJoinDmlTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private fun matches(vararg statements: String) {
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notMatches(vararg statements: String) {
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    private val update = "update employees e set e.salary = j.max_salary"

    @Test
    fun matchesUpdateFromAndUsing() {
        matches(
            "$update from jobs j where j.job_id = e.job_id;",
            "$update using jobs j where j.job_id = e.job_id;",
            "$update from jobs j;",
            "update hr.employees e set e.salary = j.max_salary from hr.jobs j where e.job_id = j.job_id;",
            "update employees set salary = jobs.max_salary from jobs where employees.job_id = jobs.job_id;",
            "update only (employees) e set e.salary = j.max_salary from jobs j where e.job_id = j.job_id;",
            "update (select * from employees) e set e.salary = j.max_salary from jobs j where e.job_id = j.job_id;",
            "update employees e set e.a = 1, e.b = j.b from jobs j where e.id = j.id;",
            "update employees e set e.a = (select max(x.a) from other x where x.id = e.id) from jobs j where j.id = e.id;"
        )
    }

    @Test
    fun matchesMultipleSourcesJoinsAndInlineViews() {
        matches(
            "$update from jobs j, depts d where j.d = d.d and e.job_id = j.job_id;",
            "$update from jobs j join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update from jobs j left join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update from jobs j cross join depts d where e.job_id = j.job_id;",
            "$update from jobs j inner join depts d using (d) where e.job_id = j.job_id;",
            "$update from (jobs j join depts d on j.d = d.d) where e.job_id = j.job_id;",
            "$update from (select job_id, max_salary from jobs) j where e.job_id = j.job_id;",
            "$update from (select job_id, max_salary from jobs) j join depts d on d.x = j.job_id where e.job_id = j.job_id;",
            "$update from jobs j, (select * from depts) d, locs l where e.job_id = j.job_id;",
            "$update from lateral (select * from jobs x where x.job_id = e.job_id) j;",
            "$update from table(my_fn(1)) j where e.job_id = j.job_id;",
            "$update from jobs@lnk j where e.job_id = j.job_id;",
            "$update from jobs as of scn 1 j where e.job_id = j.job_id;",
            "$update using jobs j join depts d on j.d = d.d, locs l where e.job_id = j.job_id;"
        )
    }

    @Test
    fun matchesRightAndFullJoinsThatOracleParsesDespiteTheDocumentation() {
        matches(
            "$update from jobs j right join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update from jobs j right outer join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update from jobs j full join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update using jobs j full outer join depts d on j.d = d.d where e.job_id = j.job_id;",
            "$update from jobs j join depts d on j.d = d.d right join locs l on l.x = d.x where e.job_id = j.job_id;",
            "$update from (jobs j full join depts d on j.d = d.d) where e.job_id = j.job_id;",
            "$update from jobs j, depts d right join locs l on l.x = d.x where e.job_id = j.job_id;",
            "delete from t from s right join u on s.b = u.b where t.a = s.a;",
            "delete from t using s full outer join u on s.b = u.b where t.a = s.a;"
        )
        notMatches(
            "$update from jobs j right depts d on j.d = d.d;",
            "delete from t from s right join;"
        )
    }

    @Test
    fun matchesDeleteFromAndUsing() {
        matches(
            "delete from t from s where t.t1 = s.s1;",
            "delete from t using s where t.t1 = s.s1;",
            "delete t from s where t.t1 = s.s1;",
            "delete t using s where t.t1 = s.s1;",
            "delete from t from s;",
            "delete from t t1 from s s1 where t1.a = s1.a;",
            "delete from t from s, u where t.a = s.a and s.b = u.b;",
            "delete from t from s join u on s.b = u.b where t.a = s.a;",
            "delete from t using s left join u on s.b = u.b where t.a = s.a;",
            "delete from t from (select a from s) x where t.a = x.a;",
            "delete from t from (s join u on s.b = u.b) where t.a = s.a;",
            "delete from only (t) from s where t.a = s.a;",
            "delete from hr.t from hr.s where t.a = s.a;"
        )
    }

    @Test
    fun keepsTrailingClausesAfterTheSourceList() {
        matches(
            "$update from jobs j where e.job_id = j.job_id returning e.salary into :x;",
            "$update from jobs j where e.job_id = j.job_id log errors into err\$_e reject limit 5;",
            "$update from jobs j where e.job_id = j.job_id returning e.salary into :x log errors reject limit unlimited;",
            "delete from t from s where t.a = s.a returning t.a into :x;",
            "delete from t from s where t.a = s.a log errors reject limit 5;",
            "delete from t from s where current of c;",
            "update employees e set e.a = 1 where e.b = 2;",
            "delete from employees where a = 1;",
            "delete from employees e where e.a = 1 returning e.a into :x;"
        )
    }

    @Test
    fun rejectsMalformedSourceLists() {
        notMatches(
            "$update from;",
            "$update from where 1 = 1;",
            "$update from jobs j,;",
            "$update from , jobs j;",
            "$update from jobs j, , depts d;",
            "$update from jobs j from depts d;",
            "$update from jobs j using depts d;",
            "$update using jobs j using depts d;",
            "$update from jobs j join;",
            "$update from jobs j join depts d;",
            "delete from t from;",
            "delete from t from s,;",
            "delete from t using;",
            "delete from t from s from u;",
            "delete from t using s from u;",
            "delete from t from s using u;"
        )
    }

    @Test
    fun rejectsMisorderedClauses() {
        notMatches(
            "update employees e set e.a = 1 where e.b = j.b from jobs j;",
            "update employees e from jobs j set e.a = j.a;",
            "update employees e set e.a = j.a from jobs j where e.b = 1 from depts d;",
            "update employees e set e.a = j.a where current of c from jobs j;",
            "$update from jobs j returning e.a into :x where e.b = 1;",
            "$update from jobs j where e.b = 1 log errors reject limit 5 returning e.a into :x;",
            "delete from t where t.a = s.a from s;",
            "delete from t where t.a = s.a using s;",
            "delete from t returning t.a into :x from s;",
            "delete from t log errors from s;"
        )
    }

    @Test
    fun keepsQueryFromClauseAndOtherDmlUnchanged() {
        matches(
            "select * from a, b where a.x = b.x;",
            "select * from a join b using (x);",
            "insert into t select * from s;",
            "merge into t using s on (t.a = s.a) when matched then update set t.b = s.b;",
            "update t set b = (select b from s where s.a = t.a);",
            "delete from t where a in (select a from s);"
        )
        notMatches(
            "select * from a using b;",
            "insert into t using s select * from s;"
        )
    }

    @Test
    fun exposesTheClauseAsItsOwnNode() {
        val tree = p.parse("update e set e.a = j.a from jobs j, depts d where j.x = d.x; " +
            "delete from t using s join u on s.b = u.b where t.a = s.a; " +
            "update e set e.a = 1; " +
            "select * from jobs j;")
        val clauses = tree.getDescendants(DmlGrammar.FROM_USING_CLAUSE)
        assertThatAst(clauses).hasSize(2)
        assertThatAst(clauses.map { it.getFirstChild().tokenOriginalValue.lowercase() }).containsExactly("from", "using")
        assertThatAst(tree.getDescendants(DmlGrammar.UPDATE_EXPRESSION).map { it.getChildren(DmlGrammar.FROM_USING_CLAUSE).size })
            .containsExactly(1, 0)
        assertThatAst(tree.getDescendants(DmlGrammar.DELETE_EXPRESSION).single().getChildren(DmlGrammar.FROM_USING_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DmlGrammar.FROM_CLAUSE)).hasSize(1)
    }
}
