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
package com.felipebz.zpa.api.dcl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DclGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RevokeStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DclGrammar.REVOKE_STATEMENT)
    }

    @Test
    fun matchesSystemPrivilegesAndRoles() {
        assertThat(p).matches("revoke drop any table from hr, oe;")
        assertThat(p).matches("revoke dw_manager from sh;")
        assertThat(p).matches("revoke create tablespace from dw_manager")
        assertThat(p).matches("revoke all privileges from user1;")
        assertThat(p).matches("revoke foo from user1, user2")
        assertThat(p).matches("revoke connect, resource from \"User One\", public")
        // A bare ALL is a role name in this branch (ORA-01924 afterwards, not a syntax error).
        assertThat(p).matches("revoke all from user1")
    }

    @Test
    fun matchesObjectPrivileges() {
        assertThat(p).matches("revoke delete on orders from hr;")
        assertThat(p).matches("revoke all on orders from hr;")
        assertThat(p).matches("revoke all privileges on orders from hr;")
        assertThat(p).matches("revoke update on emp_details_view from public;")
        assertThat(p).matches("revoke select, delete on hr.departments_seq from oe, hr;")
        assertThat(p).matches("revoke references on hr.employees from oe cascade constraints;")
        assertThat(p).matches("revoke select on t from u force")
    }

    @Test
    fun matchesOnObjectClauseForms() {
        assertThat(p).matches("revoke inherit privileges on user sh from hr;")
        assertThat(p).matches("revoke read on directory bfile_dir from hr;")
        assertThat(p).matches("revoke read on directory hr.bfile_dir from u")
        assertThat(p).matches("revoke use on edition ora\$base from u")
        assertThat(p).matches("revoke execute on mining model hr.m from u")
        assertThat(p).matches("revoke execute on java source hr.x from u")
        assertThat(p).matches("revoke execute on java resource x from u")
        assertThat(p).matches("revoke use on sql translation profile hr.p from u")
        // Without a following name these words are ordinary identifiers in Oracle 26 (ORA-00942 / ORA-01924).
        assertThat(p).matches("revoke select on directory from u")
        assertThat(p).matches("revoke r from procedure;")
        assertThat(p).matches("revoke select on \"USER\" from u")
    }

    @Test
    fun matchesSchemaPrivileges() {
        assertThat(p).matches("revoke select any table on schema hr from user1;")
        assertThat(p).matches("revoke select any table, update any table on schema hr from u container = current")
        assertThat(p).matches("revoke all privileges on schema hr from u")
    }

    @Test
    fun matchesContainerAndSuffixOrdering() {
        assertThat(p).matches("revoke foo from bar container = current")
        assertThat(p).matches("revoke select on t from u container = all")
        assertThat(p).matches("revoke references on t from u cascade constraints container = current")
        assertThat(p).matches("revoke create table from u cascade constraints")
        assertThat(p).matches("revoke select any table on schema hr from u force")
    }

    @Test
    fun matchesRolesFromPrograms() {
        assertThat(p).matches("revoke r from procedure p;")
        assertThat(p).matches("revoke r1, r2 from package hr.pkg, function f")
        assertThat(p).matches("revoke all from package hr.pkg")
    }

    @Test
    fun rejectsMissingBoundaries() {
        assertThat(p).notMatches("revoke;")
        assertThat(p).notMatches("revoke foo;")
        assertThat(p).notMatches("revoke from u;")
        assertThat(p).notMatches("revoke foo from;")
        assertThat(p).notMatches("revoke foo from u,;")
        assertThat(p).notMatches("revoke foo from , u;")
    }

    @Test
    fun rejectsMalformedObjects() {
        assertThat(p).notMatches("revoke select on from u")
        assertThat(p).notMatches("revoke select on hr. from u")
        assertThat(p).notMatches("revoke select any table on schema from u")
        assertThat(p).notMatches("revoke select on schema from u")
        // Oracle 26 rejects these at parse time: ORA-00905, ORA-01750, ORA-00990.
        assertThat(p).notMatches("revoke select on hr.a.b from u")
        assertThat(p).notMatches("revoke inherit privileges on user sh, hr from u")
        assertThat(p).notMatches("revoke update (c1, c2) on t from u")
        assertThat(p).notMatches("revoke all on schema hr from u")
        assertThat(p).notMatches("revoke select on user from u")
        assertThat(p).notMatches("revoke select on user.foo from u")
        assertThat(p).notMatches("revoke select on foo.user from u")
    }

    @Test
    fun rejectsMalformedSuffixes() {
        assertThat(p).notMatches("revoke references on t from u cascade")
        assertThat(p).notMatches("revoke references on t from u cascade constraints force")
        assertThat(p).notMatches("revoke references on t from u force cascade constraints")
        assertThat(p).notMatches("revoke references on t from u container = current cascade constraints")
        assertThat(p).notMatches("revoke select on t from u container")
        assertThat(p).notMatches("revoke select on t from u container = foo")
    }

    @Test
    fun rejectsMalformedProgramUnits() {
        assertThat(p).notMatches("revoke r from package hr.;")
        assertThat(p).notMatches("revoke r from package p container = current;")
    }
}
