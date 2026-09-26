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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class InsertStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.INSERT_STATEMENT)
    }

    @Test
    fun matchesSimpleInsert() {
        assertThat(p).matches("insert into tab values (1);")
    }

    @Test
    fun matchesInsertIntoPartition() {
        assertThat(p).matches("insert into tab partition (part1) values (1);")
        assertThat(p).matches("insert into tab subpartition (subpart1) t values (1);")
        assertThat(p).matches("insert into tab partition for (1) (col1) values (1);")
    }

    @Test
    fun matchesInsertWithTableAlias() {
        assertThat(p).matches("insert into tab t values (1);")
    }

    @Test
    fun matchesInsertWithExplicitColumn() {
        assertThat(p).matches("insert into tab (x) values (1);")
    }

    @Test
    fun matchesInsertWithExplicitColumnAlternative() {
        assertThat(p).matches("insert into tab (tab.x) values (1);")
    }

    @Test
    fun matchesInsertMultipleColumns() {
        assertThat(p).matches("insert into tab (x, y) values (1, 2);")
    }

    @Test
    fun matchesInsertWithSubquery() {
        assertThat(p).matches("insert into tab (select 1, 2 from dual);")
    }

    @Test
    fun matchesInsertWithSubqueryInColumns() {
        assertThat(p).matches("insert into tab (x, y) (select 1, 2 from dual);")
    }

    @Test
    fun matchesInsertWithSchema() {
        assertThat(p).matches("insert into sch.tab values (1);")
    }

    @Test
    fun matchesInsertRecord() {
        assertThat(p).matches("insert into tab values foo;")
    }

    @Test
    fun matchesLabeledInsert() {
        assertThat(p).matches("<<foo>> insert into tab values (1);")
    }

    @Test
    fun matchesInsertWithReturningInto() {
        assertThat(p).matches("insert into tab (x) values (1) returning x*2 into y;")
    }

    @Test
    fun matchesSimpleInsertInQuery() {
        assertThat(p).matches("insert into (select x from tab) values (1);")
    }

    @Test
    fun matchesMultiTableInsert() {
        assertThat(p).matches("insert all into tab (x) values (y) into tab (x) values (y) select 1 y from dual;")
    }

    @Test
    fun matchesMultiTableConditionalInsert() {
        assertThat(p).matches("insert all " +
            "when y < 0 then into tab (x) values (y) " +
            "when y > 0 then into tab (x) values (y) " +
            "else into tab (x) values (y) " +
            "select 1 y from dual;")
    }

    @Test
    fun matchesSimpleWithErrorLoggingClause() {
        assertThat(p).matches("insert into tab select 1, 2 from dual log errors into errlog ('oops');")
    }

    @Test
    fun matchesInsertThe() {
        assertThat(p).matches("insert into the(select x from tab) values (1);")
    }

    @Test
    fun matchesMultiRowValues() {
        assertThat(p).matches("insert into orders values (1, 'Costco', order_status.open), (2, 'BMW', default), (3, 'N', 1);")
        assertThat(p).matches("insert into t (a) values (1), ((select 2 from dual)) log errors into err;")
        assertThat(p).notMatches("insert into t (a) values (1),, (2);")
        assertThat(p).notMatches("insert into t (a) values (1), ;")
        assertThat(p).notMatches("insert into t (a) values (1) (2);")
    }

    @Test
    fun matchesInsertSet() {
        assertThat(p).matches("insert into employees set employee_id = 210, last_name = 'Smith', hire_date = sysdate;")
        assertThat(p).matches("insert into employees set (employee_id = 210, last_name = 'Smith');")
        assertThat(p).matches("insert into employees set (employee_id = 210, job_id = default), (employee_id = 211);")
        assertThat(p).matches("insert into t e set e.a = 1 returning a into x log errors into err reject limit 1;")
        // ORA-63855 / ORA-03048 / ORA-03048 / ORA-00927.
        assertThat(p).notMatches("insert into t set a = 1, (b = 2);")
        assertThat(p).notMatches("insert into t set (a = 1) (a = 2);")
        assertThat(p).notMatches("insert into t set a = 1 where a = 1;")
        assertThat(p).notMatches("insert into t set;")
    }

    @Test
    fun matchesInsertByNameOrPosition() {
        assertThat(p).matches("insert into job_history by name select employee_id, hire_date as start_date from employees;")
        assertThat(p).matches("insert into t (a, b) by position select 1, 2 from dual log errors into err;")
        assertThat(p).matches("insert into t by name with q as (select 1 a from dual) select a from q;")
        // ORA-63878: only before a subquery.
        assertThat(p).notMatches("insert into t by name values (1);")
    }

    @Test
    fun rejectsMultipleValuesRowsInMultiTableInsert() {
        // Each multi-table INTO takes a single row (ORA-00928 at the second one).
        assertThat(p).matches("insert all into t (a) values (1) into t2 (b) values (2) select * from dual;")
        assertThat(p).notMatches("insert all into t (a) values (1), (2) select * from dual;")
        assertThat(p).matches("insert first when x > 0 then into t (a) values (1) else into t2 (b) values (2) select 1 x from dual;")
        assertThat(p).notMatches("insert first when x > 0 then into t (a) values (1), (2) select 1 x from dual;")
        assertThat(p).notMatches("insert all when x > 0 then into t (a) values (1) else into t2 (b) values (2), (3) select 1 x from dual;")
    }
}
