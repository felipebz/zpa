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

class CreateAuditPolicyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_AUDIT_POLICY)
    }

    @Test
    fun matchesPrivilegesAndRoles() {
        assertThat(p).matches("create audit policy p privileges create any table, drop any table;")
        assertThat(p).matches("create audit policy p roles java_admin, java_deploy")
        assertThat(p).matches("create audit policy c##common_role1_pol roles c##role1 container = all;")
        assertThat(p).matches("create audit policy p privileges create any table container = current")
    }

    @Test
    fun matchesStandardActions() {
        assertThat(p).matches(
            "create audit policy p actions delete on hr.employees, insert on hr.employees, all on hr.departments")
        assertThat(p).matches("create audit policy p actions read on directory bfile_dir")
        assertThat(p).matches("create audit policy p actions read directory, write directory, execute directory")
        assertThat(p).matches("create audit policy p actions administer key management")
        assertThat(p).matches("create audit policy p actions insert(deptno) on scott.dept")
        assertThat(p).matches("create audit policy p actions select (c1, c2) on t, all (c1) on t")
        assertThat(p).matches("create audit policy p actions select on mining model hr.m")
        assertThat(p).matches("create audit policy p actions select on t, all, lock table")
    }

    @Test
    fun matchesComponentActions() {
        assertThat(p).matches("create audit policy p actions component = datapump import")
        assertThat(p).matches("create audit policy p actions component = datapump import, export")
        assertThat(p).matches("create audit policy p actions component = direct_load load")
        assertThat(p).matches("create audit policy p actions component = ols all")
        assertThat(p).matches("create audit policy p actions component = dv realm violation on r1, realm success on r2")
        assertThat(p).matches(
            "create audit policy p actions component = sql_firewall sql violation on u1, context violation on u2, all on u3")
        assertThat(p).matches("create audit policy p actions component = protocol http")
    }

    @Test
    fun matchesClauseCombinations() {
        assertThat(p).matches(
            "create audit policy p privileges create any table, drop any table " +
                "actions delete on hr.employees, all on hr.departments, lock table roles audit_admin, audit_viewer")
        assertThat(p).matches(
            "create audit policy p actions delete on t actions component = datapump import")
        assertThat(p).matches(
            "create audit policy p actions component = datapump import actions delete on t")
        assertThat(p).matches(
            "create audit policy p actions update on oe.orders " +
                "when 'SYS_CONTEXT(''USERENV'', ''IDENTIFICATION_TYPE'') = ''EXTERNAL''' evaluate per session")
        assertThat(p).matches(
            "create audit policy p actions select on t when 'UID = 1' evaluate per instance " +
                "only toplevel container = current")
        assertThat(p).matches("create audit policy p roles dba only toplevel")
    }

    @Test
    fun rejectsMalformedPolicies() {
        assertThat(p).notMatches("create audit policy p actions")
        assertThat(p).notMatches("create audit policy p actions select on t,")
        assertThat(p).notMatches("create audit policy p actions select on")
        assertThat(p).notMatches("create audit policy p privileges")
        assertThat(p).notMatches("create audit policy p roles")
        assertThat(p).notMatches("create audit policy p actions component = datapump")
        assertThat(p).notMatches("create audit policy p actions component = protocol smtp")
        assertThat(p).notMatches("create audit policy p actions select on t container = foo")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-46373 even before trailing tokens: at least one option, an unqualified name and a known component.
        assertThat(p).notMatches("create audit policy p")
        assertThat(p).notMatches("create audit policy p only toplevel")
        assertThat(p).notMatches("create audit policy s.p roles dba")
        assertThat(p).notMatches("create audit policy p actions component = foo import")
        // ORA-46383: PRIVILEGES, ACTIONS and ROLES must come in this order, once each.
        assertThat(p).notMatches("create audit policy p roles dba privileges create any table")
        assertThat(p).notMatches("create audit policy p actions delete on t privileges create any table")
        assertThat(p).notMatches("create audit policy p privileges create any table privileges drop any table")
        assertThat(p).notMatches("create audit policy p roles dba roles resource")
        assertThat(p).notMatches("create audit policy p actions delete on t update on t")
        assertThat(p).notMatches("create audit policy p actions select on t when 'UID = 1'")
        assertThat(p).notMatches("create audit policy p actions select on t when uid = 1 evaluate per session")
        assertThat(p).notMatches("create audit policy p actions select on t container = current only toplevel")
        assertThat(p).notMatches(
            "create audit policy p actions select on t only toplevel when 'UID = 1' evaluate per session")
    }
}
