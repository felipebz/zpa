//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class CreateAssertionTest : RuleTest() {

    private val exists = "exists (select 1 from dual)"

    @Test
    fun matchesExistentialAssertions() {
        setRootRule(DdlGrammar.CREATE_ASSERTION)
        assertThat(p).matches("create assertion a check (not exists (select 'x' from dept d " +
            "where not exists (select 'y' from emp e where e.deptno = d.deptno)));")
        assertThat(p).matches("create assertion if not exists hr.a check ($exists)")
        assertThat(p).matches("create assertion a check ($exists or not $exists)")
        assertThat(p).matches("create assertion a check (($exists) and 1 = 1)")
    }

    @Test
    fun matchesUniversalAssertions() {
        setRootRule(DdlGrammar.CREATE_ASSERTION)
        assertThat(p).matches("create assertion a check (all (select d.deptno from dept d) da " +
            "satisfy (exists (select '' from emp e where e.deptno = da.deptno)));")
        assertThat(p).matches("create assertion a check (all (select 1 x from dual) as d satisfy (d.x = 1 or d.x in (select 1 from dual)))")
        assertThat(p).matches("create assertion a check (all (select 1 x from dual) satisfy (x = 1))")
        assertThat(p).matches("create assertion a check ((all (select 1 x from dual) d satisfy (d.x = 1)))")
    }

    @Test
    fun matchesAssertionStates() {
        setRootRule(DdlGrammar.CREATE_ASSERTION)
        assertThat(p).matches("create assertion a check ($exists) novalidate;")
        assertThat(p).matches("create assertion a check ($exists) deferrable initially deferred disable novalidate")
        assertThat(p).matches("create assertion a check ($exists) initially deferred deferrable")
        assertThat(p).matches("create assertion a check ($exists) not deferrable initially immediate enable validate")
        assertThat(p).matches("create assertion a check ($exists) novalidate disable")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_ASSERTION)
        // ORA-00936 / ORA-00906 / ORA-00907.
        assertThat(p).notMatches("create assertion a check (1 = 1)")
        assertThat(p).notMatches("create assertion a check $exists")
        assertThat(p).notMatches("create assertion a check (all (select 1 x from dual) d satisfy d.x = 1)")
        assertThat(p).notMatches("create assertion a check (not all (select 1 x from dual) d satisfy (d.x = 1))")
        assertThat(p).notMatches("create assertion a check (all (select 1 x from dual) d satisfy (d.x = 1) and $exists)")
        assertThat(p).notMatches("create assertion a check (all (select 1 x from dual) d satisfy " +
            "(all (select 1 y from dual) e satisfy (e.y = d.x)))")
        // ORA-00911: table-constraint states that assertions do not have.
        assertThat(p).notMatches("create assertion a check ($exists) rely")
        assertThat(p).notMatches("create assertion a check ($exists) using index")

        // ALL ... SATISFY is not a general condition (ORA-00936).
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).notMatches("select 1 from dual where all (select 1 x from dual) d satisfy (d.x = 1)")
    }
}
