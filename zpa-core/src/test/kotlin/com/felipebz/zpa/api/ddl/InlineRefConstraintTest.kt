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

class InlineRefConstraintTest : RuleTest() {

    @Test
    fun matchesScopeAndRowid() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table d (d_no number, mgr_ref ref employees_typ scope is employees_obj_t);")
        assertThat(p).matches("create table d (r ref t scope is hr.t_tab)")
        assertThat(p).matches("create table d (r ref t scope is \"T_TAB\")")
        assertThat(p).matches("create table d (r ref t with rowid)")
        assertThat(p).matches("create table d (r ref t scope is t_tab, x number)")
        // A database link parses; Oracle rejects remote scope tables afterwards (ORA-25124).
        assertThat(p).matches("create table d (r ref t scope is t_tab@dbl)")
    }

    @Test
    fun matchesReferencesOnRefColumn() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table d (r ref t constraint mgr_in_emp references employees_obj_t)")
        assertThat(p).matches("create table d (r ref t references t_tab on delete cascade disable novalidate)")
    }

    @Test
    fun matchesMixedWithOtherConstraints() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table d (r ref t scope is t_tab with rowid)")
        assertThat(p).matches("create table d (r ref t with rowid scope is t_tab)")
        assertThat(p).matches("create table d (r ref t with rowid references t_tab)")
        assertThat(p).matches("create table d (r ref t not null scope is t_tab)")
        assertThat(p).matches("create table d (r ref t scope is t_tab not null check (r is not null))")
        assertThat(p).matches("create table d (r ref t default null scope is t_tab)")
        assertThat(p).matches("create table d (r ref t scope is t_tab annotations (a 'b'))")
        // Repetition and SCOPE with REFERENCES parse; Oracle rejects them afterwards (ORA-22888/ORA-22896).
        assertThat(p).matches("create table d (r ref t with rowid with rowid)")
        assertThat(p).matches("create table d (r ref t scope is t_tab scope is t_tab2)")
        assertThat(p).matches("create table d (r ref t scope is t_tab references t_tab)")
        // Non-REF columns parse; Oracle rejects them afterwards (ORA-22893).
        assertThat(p).matches("create table d (n number scope is t_tab)")
    }

    @Test
    fun rejectsMalformedRefConstraints() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("create table d (r ref t scope is)")
        assertThat(p).notMatches("create table d (r ref t scope is a.b.t_tab)")
        assertThat(p).notMatches("create table d (r ref t scope is (t_tab))")
        assertThat(p).notMatches("create table d (r ref t with)")
        // No constraint state after SCOPE or WITH ROWID (ORA-03076).
        assertThat(p).notMatches("create table d (r ref t scope is t_tab rely)")
        assertThat(p).notMatches("create table d (r ref t with rowid disable)")
        assertThat(p).notMatches("create table d (r ref t scope is t_tab default null)")
    }

    @Test
    fun matchesObjectTableAndAlterTableColumns() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create table d of t (r scope is t_tab);")
        assertThat(p).matches("alter table d add (r ref t scope is t_tab);")
        assertThat(p).matches("alter table d add (r ref t with rowid not null);")
        assertThat(p).matches("alter table d modify (r scope is t_tab);")
        assertThat(p).notMatches("alter table d modify (r scope t_tab);")
    }

    @Test
    fun rejectsNamedScopeOrRowid() {
        // Asserted on the column itself: CREATE TABLE's column list tolerates missing commas, so there
        // `constraint c1` would otherwise parse as a second column.
        setRootRule(DdlGrammar.TABLE_COLUMN_DEFINITION)
        assertThat(p).matches("r ref t constraint c1 references t_tab")
        // ORA-22890: SCOPE and WITH ROWID cannot be named.
        assertThat(p).notMatches("r ref t constraint c1 scope is t_tab")
        assertThat(p).notMatches("r ref t constraint c1 with rowid")
        assertThat(p).notMatches("r ref t constraint scope is t_tab")
    }
}
