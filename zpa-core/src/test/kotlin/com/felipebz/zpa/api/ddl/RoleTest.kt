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

class RoleTest : RuleTest() {

    @Test
    fun matchesCreateRole() {
        setRootRule(DdlGrammar.CREATE_ROLE)
        assertThat(p).matches("create role dw_manager;")
        assertThat(p).matches("create role if not exists r")
        assertThat(p).matches("create role r not identified")
        assertThat(p).matches("create role r identified by warehouse")
        assertThat(p).matches("create role r identified by \"pw\"")
        assertThat(p).matches("create role r identified using hr.admin")
        assertThat(p).matches("create role r identified using admin_pkg")
        assertThat(p).matches("create role r identified externally;")
        assertThat(p).matches("create role r identified globally;")
        assertThat(p).matches("create role r identified globally as 'AZURE_ROLE=WidgetManagerGroup'")
        assertThat(p).matches("create role c##role1 container = all;")
        assertThat(p).matches("create role r container = current not identified")
    }

    @Test
    fun matchesAlterRole() {
        setRootRule(DdlGrammar.ALTER_ROLE)
        assertThat(p).matches("alter role r not identified;")
        assertThat(p).matches("alter role if exists r identified by data")
        assertThat(p).matches("alter role r identified using hr.admin")
        assertThat(p).matches("alter role r identified externally")
        assertThat(p).matches("alter role r identified globally as 'dg'")
        assertThat(p).matches("alter role r not identified container = current")
        assertThat(p).matches("alter role r container = all")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_ROLE)
        // ORA-00924 / ORA-00988: a password is required and must not be a literal.
        assertThat(p).notMatches("create role r identified")
        assertThat(p).notMatches("create role r identified by 'pw'")
        // ORA-00922: user-only authentication forms and a three-part package name.
        assertThat(p).notMatches("create role r no authentication")
        assertThat(p).notMatches("create role r identified externally as 'x'")
        assertThat(p).notMatches("create role r identified by pw and factor 'x' as 'y'")
        assertThat(p).notMatches("create role r identified using a.b.c")
        assertThat(p).notMatches("create role r if not exists")

        setRootRule(DdlGrammar.ALTER_ROLE)
        // ORA-02157: ALTER ROLE needs at least one option.
        assertThat(p).notMatches("alter role r")
        assertThat(p).notMatches("alter role r if exists not identified")
    }
}
