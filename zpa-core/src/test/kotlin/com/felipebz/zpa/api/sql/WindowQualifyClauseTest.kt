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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WindowQualifyClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
    }

    @Test
    fun matchesNamedWindows() {
        assertThat(p).matches("select first_value(sal) over w from emp " +
            "window w as (partition by deptno order by sal rows unbounded preceding)")
        assertThat(p).matches("select avg(sal) over wj, avg(sal) over (wd order by sal) from emp " +
            "window wj as (partition by job), wd as (partition by deptno)")
        assertThat(p).matches("select 1 from emp window w as (partition by deptno), " +
            "w2 as (w order by sal range between unbounded preceding and current row)")
        assertThat(p).matches("select 1 from emp window w as ()")
        assertThat(p).matches("select 1 from emp e window w as (order by sal)")
    }

    @Test
    fun matchesQualify() {
        assertThat(p).matches("select ename from emp, dept where emp.deptno = dept.deptno " +
            "qualify avg(sal) over (partition by loc) > 2000 order by ename")
        assertThat(p).matches("select sal from emp where job is not null qualify sal < 2000")
        assertThat(p).matches("select avg(sal) over w as avg_sal from emp where sal > 0 window w as (partition by loc) " +
            "qualify avg_sal > (select avg(sal) from emp) order by ename fetch first 3 rows only")
        assertThat(p).matches("select job, avg(sal) from emp group by job having job is not null " +
            "window w as (order by job) qualify avg(sal) < 2000")
        assertThat(p).matches("select job from emp having job is not null group by job qualify avg(sal) > 1")
        assertThat(p).matches("select sal from emp connect by prior empno = mgr qualify sal > 1")
        assertThat(p).matches("select avg(e1.sal) over w1 as a from emp e1 window w1 as (partition by e1.mgr) " +
            "qualify a in (select avg(e2.sal) over w2 as b from emp e2 window w2 as (partition by mgr) " +
            "qualify b = avg(e.sal) over w2)")
    }

    @Test
    fun keepsWindowAndQualifyUsableAsAliases() {
        // Oracle 26 takes both words as table aliases unless `window name AS` follows.
        assertThat(p).matches("select sal from emp window where sal > 0")
        assertThat(p).matches("select sal from emp qualify where sal > 0")
        assertThat(p).matches("select window, qualify from (select 1 window, 2 qualify from dual)")
        assertThat(p).notMatches("select sal from emp qualify sal > 1")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-03049 / ORA-03048: fixed order, one WINDOW keyword, nothing but ORDER BY afterwards.
        assertThat(p).notMatches("select sal from emp e qualify sal > 1 window w as (order by sal)")
        assertThat(p).notMatches("select sal from emp e window w as (order by sal) window w2 as (order by sal)")
        assertThat(p).notMatches("select job from emp group by job qualify 1 = 1 having job is not null")
        assertThat(p).notMatches("select job from emp e window w as (order by job) connect by prior empno = mgr")
        // ORA-00907 / ORA-30485 / ORA-00906 / ORA-02000.
        assertThat(p).notMatches("select 1 from emp e window w as (order by sal partition by deptno)")
        assertThat(p).notMatches("select 1 from emp e window w as (rows unbounded preceding)")
        assertThat(p).notMatches("select 1 from emp e window w as partition by deptno")
        assertThat(p).notMatches("select 1 from emp e window w as (partition by deptno),")
        // MODEL excludes WINDOW and QUALIFY (ORA-03035 / ORA-03049).
        assertThat(p).notMatches("select sal from emp model dimension by (empno) measures (sal) rules (sal[1] = 0) " +
            "qualify sal > 1")
    }
}
