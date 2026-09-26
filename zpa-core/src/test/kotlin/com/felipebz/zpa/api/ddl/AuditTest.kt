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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class AuditTest : RuleTest() {

    @Test
    fun matchesUnifiedAudit() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        assertThat(p).matches("audit policy table_pol;")
        assertThat(p).matches("audit policy dml_pol by hr, sh")
        assertThat(p).matches("audit policy read_dir_pol except hr, sh whenever not successful")
        assertThat(p).matches("audit policy security_pol by hr whenever not successful;")
        assertThat(p).matches("audit policy p by users with granted roles dba, connect whenever successful")
        assertThat(p).matches("audit policy p by public")
        assertThat(p).matches("audit context namespace userenv attributes current_user, db_name by hr;")
        assertThat(p).matches("audit context namespace userenv attributes current_user, " +
            "context namespace c2 attributes a, b by hr, sh")
    }

    @Test
    fun matchesUnifiedNoaudit() {
        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        assertThat(p).matches("noaudit policy table_pol;")
        assertThat(p).matches("noaudit policy dml_pol by hr whenever not successful")
        assertThat(p).matches("noaudit policy p except hr")
        assertThat(p).matches("noaudit policy p by users with granted roles dba")
        assertThat(p).matches("noaudit context namespace userenv attributes current_user, db_name by hr;")
    }

    @Test
    fun matchesTraditionalNoaudit() {
        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        assertThat(p).matches("noaudit role;")
        assertThat(p).matches("noaudit select table, update table by hr, oe whenever successful")
        assertThat(p).matches("noaudit delete any table, create session")
        assertThat(p).matches("noaudit all statements")
        assertThat(p).matches("noaudit all privileges by hr")
        assertThat(p).matches("noaudit direct_path load by hr")
        assertThat(p).matches("noaudit network")
        assertThat(p).matches("noaudit role whenever successful container = all")
        assertThat(p).matches("noaudit select on hr.employees whenever successful;")
        assertThat(p).matches("noaudit alter, grant, insert, update, delete on default")
        assertThat(p).matches("noaudit read on directory bfile_dir")
        assertThat(p).matches("noaudit select on mining model hr.m")
        assertThat(p).matches("noaudit use on sql translation profile hr.p")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        // ORA-46401: traditional AUDIT is desupported.
        assertThat(p).notMatches("audit role")
        assertThat(p).notMatches("audit select on hr.employees")
        // ORA-03048: fixed order, a single unqualified policy, no WHENEVER or roles for contexts.
        assertThat(p).notMatches("audit policy s.p")
        assertThat(p).notMatches("audit policy p, p2")
        assertThat(p).notMatches("audit policy p whenever successful by hr")
        assertThat(p).notMatches("audit policy p by hr except sh")
        assertThat(p).notMatches("audit context namespace userenv attributes current_user whenever successful")
        assertThat(p).notMatches("audit context namespace userenv attributes a by users with granted roles dba")
        assertThat(p).notMatches("audit context namespace userenv")

        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        // ORA-00956 without an option; ORA-01708/ORA-01718 for BY after ON object.
        assertThat(p).notMatches("noaudit")
        assertThat(p).notMatches("noaudit select on hr.employees by hr")
    }
}
