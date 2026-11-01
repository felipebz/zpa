//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterAuditPolicyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_AUDIT_POLICY)
    }

    @Test
    fun matchesAddAndDrop() {
        assertThat(p).matches("alter audit policy p add only toplevel")
        assertThat(p).matches("alter audit policy p drop only toplevel;")
        assertThat(p).matches("alter audit policy p add privileges create any table, drop any table;")
        assertThat(p).matches("alter audit policy p add actions create java, alter java, drop java")
        assertThat(p).matches("alter audit policy p add roles dba")
        assertThat(p).matches(
            "alter audit policy p add privileges create any library, drop any library " +
                "actions delete on hr.employees, all on hr.departments roles dba, connect")
        assertThat(p).matches("alter audit policy p drop privileges create any table actions lock table roles audit_viewer")
        assertThat(p).matches("alter audit policy p add actions insert(dname) on scott.dept")
        assertThat(p).matches("alter audit policy p add roles dba only toplevel")
        // Everything after the name is optional in Oracle 26.
        assertThat(p).matches("alter audit policy p")
        assertThat(p).matches("alter audit policy p add")
    }

    @Test
    fun matchesAddDropAndCondition() {
        assertThat(p).matches(
            "alter audit policy p add actions component = datapump export drop actions component = datapump import")
        assertThat(p).matches("alter audit policy p add privileges drop any table drop privileges create any table")
        assertThat(p).matches("alter audit policy p add actions select on t actions component = datapump import")
        assertThat(p).matches("alter audit policy p condition drop;")
        assertThat(p).matches("alter audit policy p condition 'UID = 102' evaluate per statement")
        assertThat(p).matches("alter audit policy p add roles dba drop roles connect condition drop")
    }

    @Test
    fun rejectsMalformedPolicies() {
        assertThat(p).notMatches("alter audit policy")
        assertThat(p).notMatches("alter audit policy p add privileges")
        assertThat(p).notMatches("alter audit policy p add actions")
        assertThat(p).notMatches("alter audit policy p condition")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-03049: the documented `ACTIONS ADD` example is rejected at ACTIONS.
        assertThat(p).notMatches("alter audit policy p actions add insert(dname) on scott.dept")
        // ORA-46384: ADD, DROP and CONDITION come in this order, each once.
        assertThat(p).notMatches("alter audit policy p drop roles dba add roles dba")
        assertThat(p).notMatches("alter audit policy p add roles dba add actions select on t")
        assertThat(p).notMatches("alter audit policy p condition drop add roles dba")
        assertThat(p).notMatches("alter audit policy p add only toplevel roles dba")
        assertThat(p).notMatches("alter audit policy p add roles dba container = current")
        assertThat(p).notMatches("alter audit policy p condition 'UID = 1'")
        assertThat(p).notMatches("alter audit policy p condition drop evaluate per session")
        assertThat(p).notMatches("alter audit policy s.p add roles dba")
    }
}
