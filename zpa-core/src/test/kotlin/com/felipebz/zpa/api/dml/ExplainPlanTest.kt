//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.dml

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class ExplainPlanTest : RuleTest() {

    @Test
    fun matchesDocumentedExamples() {
        setRootRule(DmlGrammar.DML_COMMAND)
        assertThat(p).matches("explain plan set statement_id = 'Raise in Tokyo' into plan_table " +
            "for update employees set salary = salary * 1.10 where department_id = " +
            "(select department_id from departments where location_id = 1700);")
        assertThat(p).matches("explain plan for select * from sales " +
            "where time_id between :h and '01-OCT-2000';")
    }

    @Test
    fun matchesPrefixAndOutputTableForms() {
        setRootRule(DmlGrammar.EXPLAIN_PLAN)
        assertThat(p).matches("explain plan for select 1 from dual")
        assertThat(p).matches("explain plan set statement_id = q'[batch]' for select 1 from dual")
        assertThat(p).matches("explain plan into hr.plan_table@remote.us.example.com for select 1 from dual")
        assertThat(p).matches("explain plan set statement_id = N'batch' into plan_table for select 1 from dual")
    }

    @Test
    fun matchesEveryExplainableStatementFamily() {
        setRootRule(DmlGrammar.EXPLAIN_PLAN)
        assertThat(p).matches("explain plan for insert into t (id) values (1)")
        assertThat(p).matches("explain plan for delete from t where id = 1")
        assertThat(p).matches("explain plan for merge into t using s on (t.id = s.id) " +
            "when matched then update set id = s.id")
        assertThat(p).matches("explain plan for create table t (id number)")
        assertThat(p).matches("explain plan for create table t as select 1 id from dual")
        assertThat(p).matches("explain plan for create index ix on t (id)")
        assertThat(p).matches("explain plan for alter index ix rebuild online")
    }

    @Test
    fun rejectsInvalidPrefixAndBodies() {
        setRootRule(DmlGrammar.EXPLAIN_PLAN)
        assertThat(p).notMatches("explain plan set statement_id = 1 for select 1 from dual")
        assertThat(p).notMatches("explain plan set statement_id = :id for select 1 from dual")
        assertThat(p).notMatches("explain plan into plan_table set statement_id = 'x' for select 1 from dual")
        assertThat(p).notMatches("explain plan set statement_id = 'x' set statement_id = 'y' for select 1 from dual")
        assertThat(p).notMatches("explain plan into plan_table into plan_table for select 1 from dual")
        assertThat(p).notMatches("explain plan into a.b.c for select 1 from dual")
        assertThat(p).notMatches("explain plan select 1 from dual")
        assertThat(p).notMatches("explain plan for")
        assertThat(p).notMatches("explain plan for alter index ix rename to iy")
        assertThat(p).notMatches("explain plan for drop table t")
        assertThat(p).notMatches("explain plan for explain plan for select 1 from dual")
    }

    @Test
    fun commandAllowsExactlyOneTerminator() {
        setRootRule(DmlGrammar.DML_COMMAND)
        val statements = listOf(
            "explain plan for select 1 from dual",
            "explain plan for create table t (id number)",
            "explain plan for create index ix on t(id)",
            "explain plan for alter index ix rebuild")
        statements.forEach { statement ->
            assertThat(p).matches("$statement;")
            assertThat(p).notMatches("$statement;;")
        }
    }

}
