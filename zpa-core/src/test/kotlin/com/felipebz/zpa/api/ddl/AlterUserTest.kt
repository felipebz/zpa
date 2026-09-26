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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterUserTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_USER)
    }

    @Test
    fun matchesAuthenticationChanges() {
        assertThat(p).matches("alter user u1 identified by p3;")
        assertThat(p).matches("alter user u1 identified by p3 replace p1")
        assertThat(p).matches("alter user u identified globally as 'CN=tom,O=oracle,C=US'")
        assertThat(p).matches("alter user u identified externally as 'CN=a' with thumbprint 'abc'")
        assertThat(p).matches("alter user u no authentication")
        assertThat(p).matches("alter user u add factor 'OMA_PUSH' as 'u@x.com'")
        assertThat(p).matches("alter user u update factor 'OMA_PUSH' as 'u@x.com'")
        assertThat(p).matches("alter user u drop factor 'OMA_PUSH'")
        assertThat(p).matches("alter user user password expire http digest enable")
        assertThat(p).matches("alter user u identified by p1 digest disable")
    }

    @Test
    fun matchesOptions() {
        assertThat(p).matches("alter user sidney identified by second_2nd_pwd default tablespace example")
        assertThat(p).matches("alter user sh temporary tablespace tbs_grp_01")
        assertThat(p).matches("alter user sh profile new_profile")
        assertThat(p).matches("alter user scott quota 5000M on users")
        assertThat(p).matches("alter user sh default role all except dw_manager")
        assertThat(p).matches("alter user scott default role connect, resource")
        assertThat(p).matches("alter user u default role none")
        assertThat(p).matches("alter user u expire password rollover period")
        assertThat(p).matches("alter user u enable editions for view, synonym force")
        assertThat(p).matches("alter user u enable dictionary protection account unlock container = all")
        assertThat(p).matches("alter user u set container_data = all for sys.v\$session")
        assertThat(p).matches("alter user u add container_data = (cdb\$root, pdb1)")
        assertThat(p).matches("alter user if exists u read only")
        assertThat(p).matches("alter user u")
    }

    @Test
    fun matchesProxyClauses() {
        assertThat(p).matches("alter user app_user1 grant connect through sh with role warehouse_user")
        assertThat(p).matches("alter user app_user1 revoke connect through sh;")
        assertThat(p).matches("alter user sully grant connect through oas1 authenticated using password")
        assertThat(p).matches("alter user app_user1 grant connect through enterprise users")
        assertThat(p).matches("alter user u revoke connect through enterprise users")
        assertThat(p).matches("alter user u grant connect through p with role all except r1, r2 authentication required")
        assertThat(p).matches("alter user u grant connect through p with no roles")
        assertThat(p).matches("alter user u1, u2 grant connect through p")
        assertThat(p).matches("alter user u profile default grant connect through p")
    }

    @Test
    fun rejectsMalformedAndOracleRejectedForms() {
        assertThat(p).notMatches("alter user")
        assertThat(p).notMatches("alter user u identified by")
        assertThat(p).notMatches("alter user u default role")
        assertThat(p).notMatches("alter user u grant connect through")
        // ORA-00988 / ORA-00922: literal password, REPLACE without IDENTIFIED BY, CREATE-only AND FACTOR.
        assertThat(p).notMatches("alter user u identified by 'p1'")
        assertThat(p).notMatches("alter user u replace p0")
        assertThat(p).notMatches("alter user u identified by p1 and factor 'OMA_PUSH' as 'u@x.com'")
        // ORA-28151 / ORA-03049: a user list needs a proxy clause, and options cannot follow it.
        assertThat(p).notMatches("alter user u1, u2 profile default")
        assertThat(p).notMatches("alter user u grant connect through p profile default")
    }
}
