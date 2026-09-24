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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest

class AlterTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_TABLE)
    }

    @Test
    fun matchesAlterTableWithOutOfLineConstraint() {
        assertThat(p).matches("alter table tab add constraint c_name foreign key (f_key) references tab.col (name);")
    }

    @Test
    fun matchesAlterTableWithOutOfLineConstraintUsingAndTablespace() {
        assertThat(p).matches("alter table tab add constraint c_name primary key (col1, col2) using index;")
    }

    @Test
    fun matchesAlterTableWithOutOfLineUniqueConstraintUsingIndex() {
        assertThat(p).matches("alter table tab add constraint c_name unique (col1, col2) using index;")
    }

    @Test
    fun matchesOutOfLineRefConstraints() {
        assertThat(p).matches("alter table t add (scope for (ref_col) is scope_table)")
        assertThat(p).matches("alter table t add (scope for (ref_col) is schema_name.scope_table)")
        assertThat(p).matches("alter table t add (ref(ref_col) with rowid)")
        assertThat(p).matches("alter table t add (scope for (holder.ref_attr) is scope_table)")
        assertThat(p).matches("alter table t add (ref(holder.ref_attr) with rowid)")
        assertThat(p).matches("alter table t add (scope for (ref_col) is scope_table, extra number)")
    }

    @Test
    fun rejectsMalformedOutOfLineRefConstraints() {
        assertThat(p).notMatches("alter table t add (scope for (ref_col))")
        assertThat(p).notMatches("alter table t add (scope for ref_col is scope_table)")
        assertThat(p).notMatches("alter table t add (scope for (ref_col) is)")
        assertThat(p).notMatches("alter table t add (ref(ref_col))")
        assertThat(p).notMatches("alter table t add (ref ref_col with rowid)")
        assertThat(p).notMatches("alter table t add (ref(ref_col) with)")
    }

    @Test
    fun retainsOrdinaryRelationalPropertiesNamedScopeOrRef() {
        assertThat(p).matches("alter table t add (scope number, ref number)")
        assertThat(p).matches("alter table t add (c1 number)")
        assertThat(p).matches("alter table t add (constraint c unique (c1))")
    }

    @Test
    fun matchesAlterTableAddColumnWithDefaultOnNull() {
        assertThat(p).matches("alter table tab add col varchar2(100) default on null 'Default String';")
    }

    @Test
    fun matchesAlterTableAddColumnWithDefaultWithoutOnNull() {
        assertThat(p).matches("alter table tab add col varchar2(100) default 'Default String';")
    }

    @Test
    fun matchesAlterTableAddColumnWithDefaultOnNullForInsertOnly() {
        assertThat(p).matches("alter table tab add col varchar2(100) default on null for insert only 'Default String';")
    }

    @Test
    fun matchesAlterTableAddColumnWithDefaultOnNullForInsertAndUpdate() {
        assertThat(p).matches("alter table tab add col varchar2(100) default on null for insert and update 'Default String';")
    }

    @Test
    fun matchesAlterTableAddColumnsWithParentheses() {
        assertThat(p).matches("alter table ut_package add (last_run_id number);")
        assertThat(p).matches("alter table tab add (col1 number, col2 varchar2(100));")
        assertThat(p).matches("alter table tab add col number;")
    }

    @Test
    fun doesNotMatchAlterTableAddWithUnmatchedParentheses() {
        assertThat(p).notMatches("alter table tab add (col number;")
        assertThat(p).notMatches("alter table tab add col number);")
    }

    @Test
    fun matchesAlterTableModify() {
        assertThat(p).matches("alter table tab modify (col varchar2(350), other varchar2(4000));")
        assertThat(p).matches("alter table tab modify col varchar2(500);")
        assertThat(p).matches("alter table tab modify (col null);")
    }

    @Test
    fun matchesAlterTableModifyConstraintState() {
        assertThat(p).matches("alter table product modify constraint tc2 precheck;")
        assertThat(p).matches("alter table product modify constraint tc1 noprecheck;")
        assertThat(p).matches("alter table product modify constraint tc2 enable novalidate precheck;")
        assertThat(p).matches("alter table product modify constraint tc2 initially immediate;")
        assertThat(p).matches("alter table product modify constraint tc2 disable cascade precheck;")
        assertThat(p).matches("alter table locations modify primary key disable cascade;")
        assertThat(p).matches("alter table locations modify primary key enable;")
        assertThat(p).matches("alter table locations modify unique (country_id, location_id) disable cascade;")
    }

    @Test
    fun rejectsIncompleteAlterTableModifyConstraint() {
        assertThat(p).notMatches("alter table t modify constraint tc;")
        assertThat(p).notMatches("alter table t modify primary key;")
        assertThat(p).notMatches("alter table t modify unique (c);")
        assertThat(p).notMatches("alter table t modify constraint tc cascade;")
        assertThat(p).notMatches("alter table t modify unique () enable;")
        assertThat(p).notMatches("alter table t modify unique (c,) enable;")
        assertThat(p).notMatches("alter table t modify primary key precheck;")
        assertThat(p).notMatches("alter table t modify unique (c) precheck;")
        assertThat(p).notMatches("alter table t modify constraint tc precheck enable;")
        assertThat(p).notMatches("alter table t modify constraint tc precheck cascade;")
    }

    @Test
    fun matchesDropColumnClause() {
        assertThat(p).matches("alter table t drop (c1)")
        assertThat(p).matches("alter table t drop (c1, c2)")
        assertThat(p).matches("alter table t drop (c1) cascade constraints")
        assertThat(p).matches("alter table t drop (c1, c2) cascade constraints")
        assertThat(p).matches("alter table t drop column c1")
        assertThat(p).matches("alter table t drop column c1 cascade constraints")
        assertThat(p).matches("alter table t drop unused columns")
    }

    @Test
    fun matchesSetUnusedColumnClause() {
        assertThat(p).matches("alter table t set unused (c1)")
        assertThat(p).matches("alter table t set unused (c1, c2)")
        assertThat(p).matches("alter table t set unused column c1")
    }

    @Test
    fun rejectsMalformedDropColumnClause() {
        assertThat(p).notMatches("alter table t drop ()")
        assertThat(p).notMatches("alter table t drop (c1,)")
        assertThat(p).notMatches("alter table t set unused ()")
        assertThat(p).notMatches("alter table t drop (c1 + c2)")
        assertThat(p).notMatches("alter table t drop cascade constraints (c1)")
        assertThat(p).notMatches("alter table t drop (c1) checkpoint 1 cascade constraints")
        assertThat(p).notMatches("alter table t drop unused columns cascade constraints")
    }

    @Test
    fun doesNotCombineDropWithOtherAlterActions() {
        assertThat(p).notMatches("alter table t drop (c1) add (c2 number)")
        assertThat(p).notMatches("alter table t set unused (c1) add (c2 number)")
        assertThat(p).notMatches("alter table t drop column c1 drop (c2)")
    }

    @Test
    fun retainsExistingDropRoutes() {
        assertThat(p).matches("alter table t drop unique (email)")
        assertThat(p).matches("alter table t drop constraint pkc")
        assertThat(p).matches("alter table t drop partition p3")
    }

    @Test
    fun matchesAlterTableMove() {
        assertThat(p).matches("alter table tab move;")
        assertThat(p).matches("alter table tab move online;")
        assertThat(p).matches("alter table tab move tablespace data;")
        assertThat(p).matches("alter table tab move tablespace data online;")
        assertThat(p).matches("alter table tab move online tablespace data;")
    }

    @Test
    fun doesNotMatchAlterTableMoveWithDuplicateOnline() {
        assertThat(p).notMatches("alter table tab move online online;")
        assertThat(p).notMatches("alter table tab move online tablespace data online;")
    }

    @Test
    fun matchesAlterTableRowMovement() {
        assertThat(p).matches("alter table tab enable row movement;")
        assertThat(p).matches("alter table tab disable row movement;")
    }

    @Test
    fun doesNotMatchAlterTableAddWithoutADatatype() {
        assertThat(p).notMatches("alter table tab add (col);")
    }
}
