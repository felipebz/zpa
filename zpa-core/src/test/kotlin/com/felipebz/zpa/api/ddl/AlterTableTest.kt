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
    fun matchesAddRangePartitions() {
        assertThat(p).matches("alter table t add partition p2 values less than (200)")
        assertThat(p).matches("alter table t add partition values less than (200)")
        assertThat(p).matches("alter table t add partition p2 values less than (200), partition p3 values less than (300), partition p4 values less than (maxvalue);")
        assertThat(p).matches("alter table t add partition p2 values less than (100, 'Z')")
        assertThat(p).matches("alter table t add partition p2 values less than (to_date('2026-02-01', 'YYYY-MM-DD'))")
        assertThat(p).matches("alter table t add partition p2 values less than (200) update global indexes")
        assertThat(p).matches("alter table t add partition p2 values less than (200) tablespace ts1 lob (photo, text) store as (tablespace ts2) nested table docs store as np2")
        assertThat(p).matches("alter table t add partition p2 values less than (200), partition p3 values less than (300) tablespace ts1")
    }

    @Test
    fun rejectsMalformedAddRangePartitions() {
        assertThat(p).notMatches("alter table t add partition p2 values")
        assertThat(p).notMatches("alter table t add partition p2 values less than")
        assertThat(p).notMatches("alter table t add partition p2 values less than ()")
        assertThat(p).notMatches("alter table t add partition p2 update global indexes")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200,)")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200),")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200), p3 values less than (300)")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200), partition p3")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200) update global indexes update indexes")
        assertThat(p).notMatches("alter table t add partition p2 values less than (200) enable constraint ck")
    }

    @Test
    fun retainsColumnsNamedPartitionWithAdd() {
        assertThat(p).matches("alter table t add partition number")
        assertThat(p).matches("alter table t add (partition number)")
        assertThat(p).matches("alter table t add \"partition\" number")
    }

    @Test
    fun matchesAlterTableModify() {
        assertThat(p).matches("alter table tab modify (col varchar2(350), other varchar2(4000));")
        assertThat(p).matches("alter table tab modify col varchar2(500);")
        assertThat(p).matches("alter table tab modify (col null);")
    }

    @Test
    fun matchesAlterTableModifyColumnProperties() {
        assertThat(p).matches("alter table t modify (c collate binary_ci)")
        assertThat(p).matches("alter table t modify c collate using_nls_comp")
        assertThat(p).matches("alter table t modify (c varchar2(100) collate binary_ci)")
        assertThat(p).matches("alter table t modify (c collate binary_ci not null)")
        assertThat(p).matches("alter table t modify c annotations(label 'C')")
        assertThat(p).matches("alter table t modify (c annotations(label 'C'))")
        assertThat(p).matches("alter table t modify c annotations(add hidden, drop identity)")
        assertThat(p).matches("alter table t modify (c not null annotations(label 'C'))")
        assertThat(p).matches("alter table t modify (c1 collate binary_ci, c2 annotations(label 'C2'))")
    }

    @Test
    fun matchesAlterTableModifyColumnInlineConstraints() {
        assertThat(p).matches("alter table locations_demo modify (country_id constraint country_nn not null)")
        assertThat(p).matches("alter table t modify (c constraint c_nn not null)")
        assertThat(p).matches("alter table t modify c constraint c_nn not null")
        assertThat(p).matches("alter table t modify (c not null)")
        assertThat(p).matches("alter table t modify (c null)")
        assertThat(p).matches("alter table t modify (c number)")
        assertThat(p).matches("alter table t modify c number")
        assertThat(p).matches("alter table t modify (c default 1)")
        assertThat(p).matches("alter table t modify (c default 'x' not null)")
        assertThat(p).matches("alter table t modify (c number constraint c_nn not null)")
        assertThat(p).matches("alter table t modify (c constraint c_nn not null, c2 annotations(label 'C2'))")
        assertThat(p).matches("alter table t modify c number enable constraint c_nn")
    }

    @Test
    fun rejectsMalformedAlterTableModifyColumnProperties() {
        assertThat(p).notMatches("alter table t modify (c collate)")
        assertThat(p).notMatches("alter table t modify (c collate binary_ci binary_ai)")
        assertThat(p).notMatches("alter table t modify c annotations()")
        assertThat(p).notMatches("alter table t modify c annotations(add)")
        assertThat(p).notMatches("alter table t modify (c constraint)")
        assertThat(p).notMatches("alter table t modify (c constraint c_nn)")
        assertThat(p).notMatches("alter table t modify (c annotations(label 'C') not null)")
        assertThat(p).notMatches("alter table t modify (c annotations(label 'C') default 'x')")
    }

    @Test
    fun matchesAlterTableRenameColumn() {
        assertThat(p).matches("alter table t rename column c1 to c2")
        assertThat(p).matches("alter table schema_name.t rename column c1 to c2")
        assertThat(p).matches("alter table t rename column \"Old Name\" to \"New Name\"")
    }

    @Test
    fun rejectsMalformedAlterTableRenameColumn() {
        assertThat(p).notMatches("alter table t rename c1 to c2")
        assertThat(p).notMatches("alter table t rename column c1 c2")
        assertThat(p).notMatches("alter table t rename column to c2")
        assertThat(p).notMatches("alter table t rename column c1 to")
        assertThat(p).notMatches("alter table t rename column c1 to c2 to c3")
        assertThat(p).notMatches("alter table t rename column c1 to c2 enable constraint c1")
        assertThat(p).notMatches("alter table t rename constraint c1 to c2")
        assertThat(p).notMatches("alter table t rename to new_table")
    }

    @Test
    fun matchesAlterTableRenamePartitionOrSubpartition() {
        assertThat(p).matches("alter table t rename partition p1 to p2")
        assertThat(p).matches("alter table t rename partition \"Old Partition\" to \"New Partition\"")
        assertThat(p).matches("alter table t rename partition for (1) to p_new")
        assertThat(p).matches("alter table t rename partition for (1, 2) to p_new")
        assertThat(p).matches("alter table t rename subpartition sp1 to sp2")
        assertThat(p).matches("alter table t rename subpartition \"Old Subpartition\" to \"New Subpartition\"")
        assertThat(p).matches("alter table t rename subpartition for (1, 'A') to sp_new")
    }

    @Test
    fun rejectsMalformedAlterTableRenamePartitionOrSubpartition() {
        assertThat(p).notMatches("alter table t rename partition")
        assertThat(p).notMatches("alter table t rename partition p1")
        assertThat(p).notMatches("alter table t rename partition p1 to")
        assertThat(p).notMatches("alter table t rename partition to p2")
        assertThat(p).notMatches("alter table t rename partition for () to p2")
        assertThat(p).notMatches("alter table t rename partition for (1,) to p2")
        assertThat(p).notMatches("alter table t rename partition p1 to p2 to p3")
        assertThat(p).notMatches("alter table t rename partition p1 to p2 enable constraint ck")
        assertThat(p).notMatches("alter table t rename subpartition")
        assertThat(p).notMatches("alter table t rename subpartition sp1")
        assertThat(p).notMatches("alter table t rename subpartition sp1 to")
        assertThat(p).notMatches("alter table t rename subpartition to sp2")
        assertThat(p).notMatches("alter table t rename subpartition for () to sp2")
        assertThat(p).notMatches("alter table t rename subpartition for (1,) to sp2")
        assertThat(p).notMatches("alter table t rename subpartition sp1 to sp2 enable constraint ck")
    }

    @Test
    fun matchesExchangePartitionAndSubpartition() {
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t")
        assertThat(p).matches("alter table t exchange partition p1 with table owner.exchange_t without validation")
        assertThat(p).matches("alter table t exchange partition for (1) with table exchange_t")
        assertThat(p).matches("alter table t exchange subpartition sp1 with table exchange_t")
        assertThat(p).matches("alter table t exchange subpartition for (1, 'A') with table exchange_t")
        assertThat(p).matches("alter table t exchange partition \"Old Partition\" with table \"Exchange Table\"")
    }

    @Test
    fun matchesExchangeOptionsInOrder() {
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t including indexes with validation")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t excluding indexes without validation")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t exceptions into exceptions")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t exceptions into owner.exceptions")
        assertThat(p).matches("alter table t exchange subpartition sp1 with table exchange_t exceptions into exceptions")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t update global indexes parallel")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t without validation cascade update global indexes")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t invalidate global indexes")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t update indexes noparallel")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t cascade")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t cascade update global indexes")
        assertThat(p).matches("alter table t exchange partition p1 with table exchange_t cascade update global indexes parallel 2")
        assertThat(p).matches("alter table t exchange subpartition sp1 with table exchange_t without validation cascade")
    }

    @Test
    fun rejectsMalformedExchangePartitionAndSubpartition() {
        assertThat(p).notMatches("alter table t exchange")
        assertThat(p).notMatches("alter table t exchange partition")
        assertThat(p).notMatches("alter table t exchange partition p1")
        assertThat(p).notMatches("alter table t exchange partition p1 with")
        assertThat(p).notMatches("alter table t exchange partition p1 with table")
        assertThat(p).notMatches("alter table t exchange partition for () with table exchange_t")
        assertThat(p).notMatches("alter table t exchange subpartition for () with table exchange_t")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t including")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t indexes including")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t without")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t validation")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t without validation including indexes")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t update global indexes without validation")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t parallel 2")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t update indexes (ix (partition p1))")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t update global indexes cascade")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t update global indexes parallel 2 cascade")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t cascade including indexes")
        assertThat(p).notMatches("alter table t exchange partition p1 with table exchange_t enable constraint ck")
    }

    @Test
    fun matchesAlterTableModifyEncryption() {
        assertThat(p).matches("alter table t modify (c encrypt)")
        assertThat(p).matches("alter table t modify (c encrypt using 'AES256')")
        assertThat(p).matches("alter table t modify (c encrypt 'NOMAC')")
        assertThat(p).matches("alter table t modify (c encrypt salt)")
        assertThat(p).matches("alter table t modify (c encrypt no salt)")
        assertThat(p).matches("alter table t modify (c encrypt using 'AES256' 'NOMAC')")
        assertThat(p).matches("alter table t modify (c encrypt using 'AES256' 'NOMAC' no salt)")
        assertThat(p).matches("alter table t modify (c encrypt no salt 'NOMAC')")
        assertThat(p).matches("alter table t modify (c encrypt salt 'NOMAC')")
        assertThat(p).matches("alter table t modify (c encrypt identified by secret)")
        assertThat(p).matches("alter table t modify (c encrypt identified by 'secret')")
        assertThat(p).matches("alter table t modify (c encrypt identified by 123)")
        assertThat(p).matches("alter table t modify (c encrypt identified by null)")
        assertThat(p).matches("alter table t modify (c encrypt using 'AES256' identified by secret 'NOMAC' no salt)")
        assertThat(p).matches("alter table t modify (c varchar2(30) encrypt 'NOMAC' not null)")
        assertThat(p).matches("alter table t modify (c encrypt annotations(label 'C'))")
        assertThat(p).matches("alter table t modify (c decrypt)")
    }

    @Test
    fun rejectsMalformedAlterTableModifyEncryption() {
        assertThat(p).notMatches("alter table t modify (c encrypt using)")
        assertThat(p).notMatches("alter table t modify (c encrypt using AES256)")
        assertThat(p).notMatches("alter table t modify (c encrypt NOMAC)")
        assertThat(p).notMatches("alter table t modify (c encrypt no)")
        assertThat(p).notMatches("alter table t modify (c encrypt salt no)")
        assertThat(p).notMatches("alter table t modify (c encrypt no no salt)")
        assertThat(p).notMatches("alter table t modify (c encrypt 'NOMAC' using 'AES256')")
        assertThat(p).notMatches("alter table t modify (c encrypt identified by)")
        assertThat(p).notMatches("alter table t modify (c encrypt identified by 1 + 2)")
        assertThat(p).notMatches("alter table t modify (c encrypt identified by lower('secret'))")
        assertThat(p).notMatches("alter table t modify (c encrypt identified by secret using 'AES256')")
    }

    @Test
    fun matchesAlterTableAddEncryptedColumns() {
        assertThat(p).matches("alter table t add (c varchar2(30) encrypt)")
        assertThat(p).matches("alter table t add (c varchar2(30) encrypt using 'AES256')")
        assertThat(p).matches("alter table t add (c varchar2(30) encrypt 'NOMAC' no salt)")
        assertThat(p).matches("alter table t add (c varchar2(30) encrypt using 'AES256' 'NOMAC' no salt)")
    }

    @Test
    fun rejectsMalformedAlterTableAddEncryption() {
        assertThat(p).notMatches("alter table t add (c varchar2(30) encrypt using)")
        assertThat(p).notMatches("alter table t add (c varchar2(30) encrypt no)")
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
    fun matchesAlterTableEnableDisableConstraintTargets() {
        assertThat(p).matches("alter table t enable validate constraint c")
        assertThat(p).matches("alter table t enable novalidate constraint c")
        assertThat(p).matches("alter table t enable constraint c")
        assertThat(p).matches("alter table t disable constraint c")
        assertThat(p).matches("alter table t disable validate constraint c")
        assertThat(p).matches("alter table t disable novalidate constraint c")
        assertThat(p).matches("alter table t enable primary key")
        assertThat(p).matches("alter table t disable primary key cascade")
        assertThat(p).matches("alter table t enable unique (c1)")
        assertThat(p).matches("alter table t enable unique (c1, c2)")
        assertThat(p).matches("alter table t disable unique (c1, c2)")
    }

    @Test
    fun matchesAlterTableEnableDisableSuffixes() {
        assertThat(p).matches("alter table t enable primary key using index")
        assertThat(p).matches("alter table t enable constraint c using index existing_idx")
        assertThat(p).matches("alter table t enable constraint c exceptions into exceptions")
        assertThat(p).matches("alter table t enable primary key using index exceptions into owner.exceptions")
        assertThat(p).matches("alter table t disable constraint c cascade")
        assertThat(p).matches("alter table t disable primary key keep index")
        assertThat(p).matches("alter table t disable primary key drop index")
        assertThat(p).matches("alter table t disable primary key cascade keep index")
    }

    @Test
    fun matchesRepeatedEnableDisableClausesAfterOptionalAction() {
        assertThat(p).matches("alter table t enable novalidate primary key enable novalidate constraint c")
        assertThat(p).matches("alter table t enable constraint c1 disable constraint c2")
        assertThat(p).matches("alter table t disable constraint c1 enable constraint c2")
        assertThat(p).matches("alter table t add (c2 number) enable constraint c")
        assertThat(p).matches("alter table t add (c2 number) enable constraint c1 disable constraint c2")
        assertThat(p).matches("alter table t modify c2 number enable constraint c")
        assertThat(p).matches("alter table t modify (c2 number) enable constraint c")
        assertThat(p).matches("alter table t enable row movement enable constraint c")
        assertThat(p).matches("alter table t modify constraint c disable")
    }

    @Test
    fun rejectsMalformedEnableDisableClauses() {
        assertThat(p).notMatches("alter table t enable")
        assertThat(p).notMatches("alter table t disable")
        assertThat(p).notMatches("alter table t enable novalidate")
        assertThat(p).notMatches("alter table t enable constraint")
        assertThat(p).notMatches("alter table t enable primary")
        assertThat(p).notMatches("alter table t enable unique ()")
        assertThat(p).notMatches("alter table t enable unique (c1,)")
        assertThat(p).notMatches("alter table t validate enable constraint c")
        assertThat(p).notMatches("alter table t enable constraint c validate")
        assertThat(p).notMatches("alter table t disable constraint c cascade exceptions into exceptions")
        assertThat(p).notMatches("alter table t enable primary key cascade")
        assertThat(p).notMatches("alter table t enable primary key keep index")
        assertThat(p).notMatches("alter table t disable primary key using index")
        assertThat(p).notMatches("alter table t disable constraint c exceptions into exceptions")
        assertThat(p).notMatches("alter table t enable all triggers")
        assertThat(p).notMatches("alter table t enable table lock")
        assertThat(p).notMatches("alter table t enable constraint c add (c2 number)")
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
        assertThat(p).notMatches("alter table t drop constraint ck add (c2 number)")
    }

    @Test
    fun retainsExistingDropRoutes() {
        assertThat(p).matches("alter table t drop unique (email)")
        assertThat(p).matches("alter table t drop constraint pkc")
        assertThat(p).matches("alter table t drop partition p3")
    }

    @Test
    fun matchesDropConstraintClause() {
        assertThat(p).matches("alter table t drop primary key")
        assertThat(p).matches("alter table t drop primary key cascade")
        assertThat(p).matches("alter table t drop primary key keep index")
        assertThat(p).matches("alter table t drop primary key drop index")
        assertThat(p).matches("alter table t drop primary key online")
        // Oracle parses CASCADE with ONLINE, then rejects the combination with ORA-14419.
        assertThat(p).matches("alter table t drop primary key cascade keep index online")
        assertThat(p).matches("alter table t drop primary key cascade drop index online")

        assertThat(p).matches("alter table t drop unique (email)")
        assertThat(p).matches("alter table t drop unique (first_name, last_name)")
        assertThat(p).matches("alter table t drop unique (email) cascade")
        assertThat(p).matches("alter table t drop unique (email) keep index")
        assertThat(p).matches("alter table t drop unique (email) drop index")
        assertThat(p).matches("alter table t drop unique (email) online")
        assertThat(p).matches("alter table t drop unique (email) keep index online")

        assertThat(p).matches("alter table t drop constraint ck")
        assertThat(p).matches("alter table t drop constraint ck cascade")
        assertThat(p).matches("alter table t drop constraint ck online")
        assertThat(p).matches("alter table t drop constraint ck cascade online")
        assertThat(p).matches("alter table t drop constraint pk keep index")
        assertThat(p).matches("alter table t drop constraint pk drop index")
    }

    @Test
    fun matchesRepeatedConstraintDrops() {
        assertThat(p).matches("alter table t drop primary key drop constraint ck")
        assertThat(p).matches("alter table t drop unique (c1) drop unique (c2)")
    }

    @Test
    fun allowsConstraintDropFollowedByEnable() {
        assertThat(p).matches("alter table t drop constraint ck enable constraint other_ck")
    }

    @Test
    fun rejectsMalformedDropConstraintClause() {
        assertThat(p).notMatches("alter table t drop primary")
        assertThat(p).notMatches("alter table t drop unique ()")
        assertThat(p).notMatches("alter table t drop unique (c1,)")
        assertThat(p).notMatches("alter table t drop constraint")
        assertThat(p).notMatches("alter table t drop primary key cascade constraints")
        assertThat(p).notMatches("alter table t drop primary key online cascade")
        assertThat(p).notMatches("alter table t drop constraint ck online cascade")
        assertThat(p).notMatches("alter table t drop unique (c1) online keep index")
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
    fun matchesMoveTablePartition() {
        assertThat(p).matches("alter table t move partition p1")
        assertThat(p).matches("alter table t move partition p1 tablespace ts2")
        assertThat(p).matches("alter table t move partition for (1) tablespace ts2")
        assertThat(p).matches("alter table t move partition p1 mapping table")
        assertThat(p).matches("alter table t move partition p1 mapping table tablespace ts2")
        assertThat(p).matches("alter table t move partition p1 lob (photo) store as (tablespace ts2) nested table docs store as nt_p1")
        assertThat(p).matches("alter table t move partition p1 tablespace ts2 update indexes")
        assertThat(p).matches("alter table t move partition p1 tablespace ts2 update global indexes")
        assertThat(p).matches("alter table t move partition p1 update indexes (ix (partition p1 tablespace ts2))")
        assertThat(p).matches("alter table t move partition p1 parallel")
        assertThat(p).matches("alter table t move partition p1 parallel 2")
        assertThat(p).matches("alter table t move partition p1 noparallel")
        assertThat(p).matches("alter table t move partition p1 online")
        assertThat(p).matches("alter table t move partition p1 tablespace ts2 online")
        assertThat(p).matches("alter table t move partition p1 update indexes parallel 2 online")
        assertThat(p).matches("alter table t move partition p1 update indexes tablespace ts2")
        assertThat(p).matches("alter table t move partition p1 parallel 2 tablespace ts2")
        assertThat(p).matches("alter table t move partition p1 online tablespace ts2")
        assertThat(p).matches("alter table t move partition p1 parallel 2 update indexes")
        assertThat(p).matches("alter table t move partition p1 online update indexes")
        assertThat(p).matches("alter table t move partition p1 online parallel 2")
        assertThat(p).matches("alter table t move partition p1 online tablespace ts2 parallel 2 update indexes")
        assertThat(p).matches("alter table t move partition p1 tablespace ts2 pctfree 10 online")
        assertThat(p).matches("alter table t move partition p1 update indexes parallel 2 tablespace ts2 online")
        assertThat(p).matches("alter table t move partition p1 online lob (photo) store as (tablespace ts2)")
        assertThat(p).matches("alter table t move partition p1 online update indexes (ix (partition p1 tablespace ts2))")
        assertThat(p).matches("alter table t move partition p1 update indexes (ix (partition p1 tablespace ts2)) tablespace ts2")
    }

    @Test
    fun rejectsMalformedMoveTablePartition() {
        assertThat(p).notMatches("alter table t move partition")
        assertThat(p).notMatches("alter table t move partition for ()")
        assertThat(p).notMatches("alter table t move partition for (1,)")
        assertThat(p).notMatches("alter table t move partition p1 tablespace")
        assertThat(p).notMatches("alter table t move partition p1 mapping")
        assertThat(p).notMatches("alter table t move partition p1 update")
        assertThat(p).notMatches("alter table t move partition p1 parallel 2 online online")
        assertThat(p).notMatches("alter table t move partition p1 online online")
        assertThat(p).notMatches("alter table t move partition p1 parallel 2 parallel 4")
        assertThat(p).notMatches("alter table t move partition p1 update indexes update indexes")
        assertThat(p).notMatches("alter table t move partition p1 tablespace ts2 tablespace ts2")
        assertThat(p).notMatches("alter table t move partition p1 tablespace ts2 pctfree 10 tablespace ts2")
        assertThat(p).notMatches("alter table t move partition p1 online update indexes online")
        assertThat(p).notMatches("alter table t move partition p1 parallel 2 update indexes noparallel")
        assertThat(p).notMatches("alter table t move partition p1 enable constraint ck")
        assertThat(p).notMatches("alter table t move subpartition sp1")
    }

    @Test
    fun matchesAlterTableRowMovement() {
        assertThat(p).matches("alter table tab enable row movement;")
        assertThat(p).matches("alter table tab disable row movement;")
    }

    @Test
    fun matchesSplitTablePartitionPayloads() {
        assertThat(p).matches("alter table t split partition p1 at (100) into (partition p1a, partition p1b)")
        assertThat(p).matches("alter table t split partition p1 at (to_date('2026-01-01', 'yyyy-mm-dd')) into (partition p1a, partition p1b)")
        assertThat(p).matches("alter table t split partition p1 at (100, 200) into (partition p1a, partition p1b)")
        assertThat(p).matches("alter table t split partition p1 values ('A', 'B') into (partition p_a, partition p1)")
        assertThat(p).matches("alter table t split partition p1 values (('A', 1), ('B', 2)) into (partition p_a, partition p1)")
        assertThat(p).matches("alter table t split partition p1 into (partition p_a values less than (100), partition p_b values less than (200), partition p_c)")
        assertThat(p).matches("alter table t split partition p1 into (partition p_a values ('A'), partition p_b values ('B'), partition p_c)")
        assertThat(p).matches("alter table t split partition p1 into (partition p1a tablespace tbs1, partition p1b tablespace tbs2)")
        assertThat(p).matches("alter table t split partition for (1) at (100) into (partition p1a, partition p1b)")
        assertThat(p).matches("alter table t split partition for (to_date('2026-01-01', 'yyyy-mm-dd')) at (100)")
    }

    @Test
    fun matchesSplitPartitionStorageAndIndexSuffixes() {
        assertThat(p).matches("alter table t split partition p1 at (150) into (partition p1a tablespace ts1 lob (photo, text) store as (tablespace ts2), partition p1b lob (photo, text) store as (tablespace ts2)) nested table docs into (partition np1, partition np2)")
        assertThat(p).matches("alter table t split partition p1 at (100) into (partition p1a, partition p1b) update global indexes")
        assertThat(p).matches("alter table t split partition p1 at (100) into (partition p1a, partition p1b) invalidate global indexes")
        assertThat(p).matches("alter table t split partition p1 at (100) into (partition p1a, partition p1b) update indexes (ix (partition p1a tablespace ts1, partition p1b tablespace ts2))")
        assertThat(p).matches("alter table t split partition p1 into (partition p1a tablespace ts1, partition p1b tablespace ts2) update indexes")
        assertThat(p).matches("alter table t split partition p1 at (100) update indexes noparallel online")
    }

    @Test
    fun rejectsMalformedSplitPartitionSuffixes() {
        assertThat(p).notMatches("alter table t split partition p1 at (100) update indexes ()")
        assertThat(p).notMatches("alter table t split partition p1 at (100) update global indexes update indexes")
        assertThat(p).notMatches("alter table t split partition p1 at (100) noparallel update indexes")
        assertThat(p).notMatches("alter table t split partition p1 at (100) online update indexes")
        assertThat(p).notMatches("alter table t split partition p1 at (100) nested table docs into (partition np1)")
        assertThat(p).notMatches("alter table t split partition p1 at (100) into (partition p1a, partition p1b) enable constraint ck")
        assertThat(p).notMatches("alter table t split partition p1 at (100) enable constraint ck")
    }

    @Test
    fun rejectsMalformedSplitTablePartition() {
        assertThat(p).notMatches("alter table t split")
        assertThat(p).notMatches("alter table t split partition")
        assertThat(p).notMatches("alter table t split partition p1")
        assertThat(p).notMatches("alter table t split partition p1 at ()")
        assertThat(p).notMatches("alter table t split partition p1 values ()")
        assertThat(p).notMatches("alter table t split partition p1 at (1,)")
        assertThat(p).notMatches("alter table t split partition p1 values ('A',)")
        assertThat(p).notMatches("alter table t split partition p1 values (('A', 1),)")
        assertThat(p).notMatches("alter table t split partition for () at (1)")
        assertThat(p).notMatches("alter table t split partition p1 at (1) into (partition p1a)")
        assertThat(p).notMatches("alter table t split partition p1 values ('A') into (partition p1a)")
        assertThat(p).notMatches("alter table t split partition p1 at (1) into (partition p1a, partition p1b, partition p1c)")
        assertThat(p).notMatches("alter table t split partition p1 values ('A') into (partition p1a, partition p1b, partition p1c)")
        assertThat(p).notMatches("alter table t split partition p1 into (partition p_a values less than (100), partition p_b values less than (200),)")
        assertThat(p).notMatches("alter table t split partition p1 into (partition p_a, partition p_b)")
        assertThat(p).notMatches("alter table t split partition p1 into (partition p_a tablespace ts1, partition p_b tablespace ts2, partition p_c tablespace ts3)")
    }

    @Test
    fun matchesMergeTablePartitionInputFamilies() {
        assertThat(p).matches("alter table t merge partitions p1, p2 into partition p12")
        assertThat(p).matches("alter table t merge partitions p1, p2, p3 into partition p123")
        assertThat(p).matches("alter table t merge partitions p1 to p4 into partition p_all")
        assertThat(p).matches("alter table t merge partitions p1, p2")
        assertThat(p).matches("alter table t merge partitions for (1), for (11) into partition p12")
        assertThat(p).matches("alter table t merge partitions p1, for (11) into partition p12")
        assertThat(p).matches("alter table t merge partitions for (to_date('2026-01-15','yyyy-mm-dd')), for (date '2026-02-15') into partition p12")
        // Oracle generates the result name when the optional partition_spec name is omitted.
        assertThat(p).matches("alter table t merge partitions p1, p2 into partition")
    }

    @Test
    fun matchesMergeTablePartitionSuffixes() {
        assertThat(p).matches("alter table t merge partitions p1, p2 into partition p12 tablespace ts1")
        assertThat(p).matches("alter table t merge partitions p2a, p2b into partition p2ab tablespace example nested table docs store as nt_p2ab")
        assertThat(p).matches("alter table t merge partitions p1, p2 update global indexes")
        assertThat(p).matches("alter table t merge partitions p1, p2 invalidate global indexes")
        assertThat(p).matches("alter table t merge partitions p1, p2 update indexes (ix (partition p12 tablespace ts1)) parallel 2 online")
        assertThat(p).matches("alter table t merge partitions p1 to p4 into partition p_all noparallel")
    }

    @Test
    fun rejectsMalformedMergeTablePartitions() {
        assertThat(p).notMatches("alter table t merge")
        assertThat(p).notMatches("alter table t merge partitions")
        assertThat(p).notMatches("alter table t merge partitions p1")
        assertThat(p).notMatches("alter table t merge partitions p1,")
        assertThat(p).notMatches("alter table t merge partitions p1, into partition p_new")
        assertThat(p).notMatches("alter table t merge partitions p1 to")
        assertThat(p).notMatches("alter table t merge partitions to p4")
        assertThat(p).notMatches("alter table t merge partitions p1, p2,")
        assertThat(p).notMatches("alter table t merge partitions p1, p2 to p3")
        assertThat(p).notMatches("alter table t merge partitions p1 to p3, p4")
        assertThat(p).notMatches("alter table t merge partitions p1, p2 into")
        assertThat(p).notMatches("alter table t merge partitions p1, for ()")
        assertThat(p).notMatches("alter table t merge partitions p1, for (1,)")
        assertThat(p).notMatches("alter table t merge partitions p1, p2 into partition p12 enable constraint ck")
        assertThat(p).notMatches("alter table t merge partitions p1, p2 online update indexes")
        assertThat(p).notMatches("alter table t merge partitions p1, p2 noparallel update indexes")
        assertThat(p).notMatches("alter table t merge partitions p2a, p2b into partition p2ab nested table docs store as")
    }

    @Test
    fun matchesModifyPartitionLocalIndexes() {
        assertThat(p).matches("alter table t modify partition p1 unusable local indexes")
        assertThat(p).matches("alter table t modify partition p1 rebuild unusable local indexes")
        assertThat(p).matches("alter table t modify partition for (1) unusable local indexes")
        assertThat(p).matches("alter table t modify partition for (1) rebuild unusable local indexes")
        assertThat(p).matches("alter table t modify (partition number)")
        assertThat(p).matches("alter table t modify \"partition\" number")
    }

    @Test
    fun rejectsMalformedModifyPartitionLocalIndexes() {
        assertThat(p).notMatches("alter table t modify partition")
        assertThat(p).notMatches("alter table t modify partition p1")
        assertThat(p).notMatches("alter table t modify partition p1 rebuild")
        assertThat(p).notMatches("alter table t modify partition p1 unusable")
        assertThat(p).notMatches("alter table t modify partition p1 unusable local")
        assertThat(p).notMatches("alter table t modify partition p1 rebuild unusable")
        assertThat(p).notMatches("alter table t modify partition p1 rebuild local indexes")
        assertThat(p).notMatches("alter table t modify partition p1 rebuild rebuild unusable local indexes")
        assertThat(p).notMatches("alter table t modify partition p1 local indexes unusable")
        assertThat(p).notMatches("alter table t modify partition p1 unusable indexes local")
        assertThat(p).notMatches("alter table t modify partition p1 unusable rebuild local indexes")
        assertThat(p).notMatches("alter table t modify partition for () unusable local indexes")
        assertThat(p).notMatches("alter table t modify partition p1 unusable local indexes tablespace users")
        assertThat(p).notMatches("alter table t modify partition p1 rebuild unusable local indexes indexing on")
        assertThat(p).notMatches("alter table t modify partition p1 unusable local indexes enable constraint ck")
    }

    @Test
    fun doesNotMatchAlterTableAddWithoutADatatype() {
        assertThat(p).notMatches("alter table tab add (col);")
    }
}
