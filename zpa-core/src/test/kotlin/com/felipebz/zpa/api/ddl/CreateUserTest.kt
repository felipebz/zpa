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

class CreateUserTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_USER)
    }

    @Test
    fun matchesAuthentication() {
        assertThat(p).matches("create user u1 identified by p1 profile prof1;")
        assertThat(p).matches("create user u identified by \"p 1\"")
        assertThat(p).matches("create user u identified by p1 http digest enable")
        assertThat(p).matches("create user u identified by p1 digest disable and factor 'OMA_PUSH' as 'u@example.com'")
        assertThat(p).matches("create user app_user1 identified externally")
        assertThat(p).matches("create user u identified externally as 'CN=foo' with thumbprint 'abc'")
        assertThat(p).matches("create user u identified globally as 'AZURE_USER=peter.fitch@example.com'")
        assertThat(p).matches("create user u identified globally")
        assertThat(p).matches("create user u no authentication")
        assertThat(p).matches("create user if not exists c##comm_user identified by comm_pwd")
    }

    @Test
    fun matchesOptions() {
        assertThat(p).matches(
            "create user sidney identified by out_standing1 default tablespace example quota 10M on example " +
                "temporary tablespace temp quota 5M on system profile app_user password expire;")
        assertThat(p).matches("create user u identified by p quota unlimited on users quota 10 on t2 quota 10 m on t3")
        assertThat(p).matches("create user u local temporary tablespace temp default collation binary_ci")
        assertThat(p).matches(
            "create user u identified by p account lock enable editions container = current read only read write")
        assertThat(p).matches("create user u password expire password expire")
        // Oracle 26 accepts the options in any order, and none is required.
        assertThat(p).matches("create user u profile default identified by p1")
        assertThat(p).matches("create user u default tablespace users")
        assertThat(p).matches("create user u")
    }

    @Test
    fun rejectsMalformedUsers() {
        assertThat(p).notMatches("create user")
        assertThat(p).notMatches("create user u identified")
        assertThat(p).notMatches("create user u identified by")
        assertThat(p).notMatches("create user u quota on users")
        assertThat(p).notMatches("create user u quota 10M users")
        assertThat(p).notMatches("create user u account")
        assertThat(p).notMatches("create user u container = foo")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-00988 / ORA-28025: literal password, quoted-identifier external name.
        assertThat(p).notMatches("create user u identified by 'p1'")
        assertThat(p).notMatches("create user u identified externally as \"CN=foo\"")
    }
}
