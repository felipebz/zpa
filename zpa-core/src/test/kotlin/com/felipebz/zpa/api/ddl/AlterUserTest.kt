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
import org.assertj.core.api.Assertions.assertThat as assertThatAst

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

    @Test
    fun matchesEnableEditionsForObjectTypes() {
        listOf(
            "enable editions", "enable editions force", "enable editions for sql translation profile",
            "enable editions for view", "enable editions for view, synonym", "enable editions for package, type",
            "enable editions for procedure, function, trigger, library",
            "enable editions for sql translation profile, view", "enable editions for view, sql translation profile force",
        ).forEach { assertThat(p).describedAs(it).matches("alter user u $it") }
        assertThat(p).matches("ALTER USER username ENABLE EDITIONS FOR SQL TRANSLATION PROFILE;")
    }

    @Test
    fun rejectsMalformedOrUnknownEditionableTypes() {
        listOf(
            "enable editions for", "enable editions for view,", "enable editions for , view", "enable editions for view synonym",
            "enable editions for view,,synonym", "enable editions for definitely_not_a_type", "enable editions for table",
            "enable editions for materialized view", "enable editions for package body", "enable editions for type body",
            "enable editions for sql translation", "enable editions for sql translation profile profile",
            "enable editions force for view", "enable editions for view force force", "enable editions for view for synonym",
        ).forEach { assertThat(p).describedAs(it).notMatches("alter user u $it") }
    }

    @Test
    fun buildsEnableEditionsAsFlatTokens() {
        val bare = p.parse("alter user u enable editions")
        assertThatAst(bare.children.map { it.name }).containsExactly("ALTER", "USER", "IDENTIFIER_NAME", "ENABLE", "EDITIONS")
        val typed = p.parse("alter user username enable editions for sql translation profile;")
        assertThatAst(typed.children.map { it.name }).containsExactly(
            "ALTER", "USER", "IDENTIFIER_NAME", "ENABLE", "EDITIONS", "FOR", "SQL", "TRANSLATION", "PROFILE", "SEMICOLON")
    }
}
