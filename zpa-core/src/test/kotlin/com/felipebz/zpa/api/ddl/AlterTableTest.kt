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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
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
        assertThat(p).notMatches("alter table t add (ref(ref_col) with)")
    }

    @Test
    fun retainsOrdinaryRelationalPropertiesNamedScopeOrRef() {
        assertThat(p).matches("alter table t add (scope number, ref number)")
        assertThat(p).matches("alter table t add (c1 number)")
        assertThat(p).matches("alter table t add (constraint c unique (c1))")
        // A column named REF with an inline WITH ROWID parses; Oracle rejects it afterwards because the
        // column is not a REF (ORA-22893).
        assertThat(p).matches("alter table t add (ref ref_col with rowid)")
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
        assertThat(p).notMatches("alter table t rename constraint c1")
        assertThat(p).notMatches("alter table t rename constraint c1 to")
        assertThat(p).notMatches("alter table t rename to")
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
    fun matchesTruncatePartitionAndSubpartitionSelectors() {
        assertThat(p).matches("alter table t truncate partition p1")
        assertThat(p).matches("alter table t truncate partition \"Old Partition\"")
        assertThat(p).matches("alter table t truncate partition p1, p2")
        assertThat(p).matches("alter table t truncate partitions p1")
        assertThat(p).matches("alter table t truncate partitions p1, p2, p3")
        assertThat(p).matches("alter table t truncate partition for (1)")
        assertThat(p).matches("alter table t truncate partitions for (1 + 1), for (101)")
        assertThat(p).matches("alter table t truncate subpartition sp1")
        assertThat(p).matches("alter table t truncate subpartition sp1, sp2")
        assertThat(p).matches("alter table t truncate subpartitions sp1")
        assertThat(p).matches("alter table t truncate subpartitions sp1, sp2")
        assertThat(p).matches("alter table t truncate subpartition for (1, 'A')")
        assertThat(p).matches("alter table t truncate subpartitions for (1, 'A'), for (1, 'B')")
    }

    @Test
    fun matchesTruncatePartitionSuffixes() {
        assertThat(p).matches("alter table t truncate partition p1 drop storage")
        assertThat(p).matches("alter table t truncate partition p1 drop all storage")
        assertThat(p).matches("alter table t truncate partition p1 reuse storage")
        assertThat(p).matches("alter table t truncate partition p1 update global indexes")
        assertThat(p).matches("alter table t truncate partition p1 invalidate global indexes")
        assertThat(p).matches("alter table t truncate partition p1 update indexes")
        assertThat(p).matches("alter table t truncate partition p1 update global indexes parallel 2")
        assertThat(p).matches("alter table t truncate partition p1 update indexes noparallel")
        assertThat(p).matches("alter table t truncate partition p1 cascade")
        assertThat(p).matches("alter table t truncate subpartition sp1 cascade")
        assertThat(p).matches("alter table t truncate partition p1 drop storage cascade update global indexes parallel 2")
    }

    @Test
    fun matchesTableAnnotations() {
        assertThat(p).matches("alter table t annotations(drop Operations, drop Hidden)")
        assertThat(p).matches("alter table t annotations(add Operations '[\"Sort\", \"Group\"]')")
        assertThat(p).matches("alter table t annotations(replace Display 'New value')")
        assertThat(p).matches("alter table t annotations(add or replace A 'x', drop if exists B, add if not exists C)")
        assertThat(p).matches("alter table t annotations(Hidden);")
        // ALTER TABLE ADD column is not a CREATE statement: Oracle parses these directives there.
        assertThat(p).matches("alter table t add (d number annotations(drop Foo))")
        assertThat(p).matches("alter table t add (e number annotations(add or replace Foo 'x'))")
    }

    @Test
    fun rejectsMalformedTableAnnotations() {
        assertThat(p).notMatches("alter table t annotations")
        assertThat(p).notMatches("alter table t annotations()")
        assertThat(p).notMatches("alter table t annotations(drop)")
        assertThat(p).notMatches("alter table t annotations(replace)")
        assertThat(p).notMatches("alter table t annotations(Display 'x',)")
    }

    @Test
    fun rejectsMalformedTruncatePartitionAndSubpartition() {
        assertThat(p).notMatches("alter table t truncate")
        assertThat(p).notMatches("alter table t truncate partition")
        assertThat(p).notMatches("alter table t truncate partitions")
        assertThat(p).notMatches("alter table t truncate partition for ()")
        assertThat(p).notMatches("alter table t truncate partition for (1,)")
        assertThat(p).notMatches("alter table t truncate partitions p1,")
        assertThat(p).notMatches("alter table t truncate partitions p1, for (2)")
        assertThat(p).notMatches("alter table t truncate partitions for (1), p2")
        assertThat(p).notMatches("alter table t truncate partitions partition p1, partition p2")
        assertThat(p).notMatches("alter table t truncate subpartition")
        assertThat(p).notMatches("alter table t truncate subpartitions")
        assertThat(p).notMatches("alter table t truncate subpartition for ()")
        assertThat(p).notMatches("alter table t truncate subpartitions sp1,")
        assertThat(p).notMatches("alter table t truncate subpartitions sp1, for (1, 'B')")
        assertThat(p).notMatches("alter table t truncate subpartitions for (1, 'A'), sp2")
        assertThat(p).notMatches("alter table t truncate partition p1 drop")
        assertThat(p).notMatches("alter table t truncate partition p1 drop all")
        assertThat(p).notMatches("alter table t truncate partition p1 reuse")
        assertThat(p).notMatches("alter table t truncate partition p1 all storage")
        assertThat(p).notMatches("alter table t truncate partition p1 parallel 2")
        assertThat(p).notMatches("alter table t truncate partition p1 update indexes (ix (partition p1))")
        assertThat(p).notMatches("alter table t truncate partition p1 drop storage update global indexes cascade")
        assertThat(p).notMatches("alter table t truncate partition p1 cascade drop storage")
        assertThat(p).notMatches("alter table t truncate partition p1 enable constraint ck")
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
        assertThat(p).notMatches("alter table t enable all")
        assertThat(p).notMatches("alter table t enable table")
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
    fun matchesAlterTablePhysicalAndStorageProperties() {
        assertThat(p).matches("alter table employees pctfree 30 pctused 60;")
        assertThat(p).matches("alter table countries_demo initrans 4 maxtrans 10 storage (next 1m);")
        assertThat(p).matches("alter table customers parallel;")
        assertThat(p).matches("alter table employees parallel 8;")
        assertThat(p).matches("alter table employees noparallel nologging;")
        assertThat(p).matches("alter table employees allocate extent (size 5k instance 4);")
        assertThat(p).matches("alter table employees deallocate unused keep 1m;")
        assertThat(p).matches("alter table t row store compress advanced;")
        assertThat(p).matches("alter table t column store compress for query high no row level locking;")
        assertThat(p).matches("alter table t cache minimize records_per_block;")
        // Oracle 26 accepts the properties in any order and together.
        assertThat(p).matches(
            "alter table t pctfree 10 parallel 4 nologging cache result_cache (mode default) enable row movement;")
        assertThat(p).matches("alter table t enable row movement nologging pctfree 10;")
    }

    @Test
    fun matchesAlterTableResultCacheAndReplication() {
        assertThat(p).matches("alter table employee result_cache (mode default)")
        assertThat(p).matches("alter table employee result_cache (standby enable)")
        assertThat(p).matches("alter table employee result_cache (mode default, standby enable)")
        assertThat(p).matches("alter table employee result_cache (standby enable, mode force)")
        assertThat(p).matches("alter table t enable logical replication allow novalidate keys no partial json")
        assertThat(p).matches("alter table t disable logical replication")
        assertThat(p).matches("alter table t upgrade not including data")
    }

    @Test
    fun matchesAlterTableInMemoryClauses() {
        assertThat(p).matches("alter table customer inmemory;")
        assertThat(p).matches("alter table customer no inmemory;")
        assertThat(p).matches("alter table customer inmemory memcompress for query low priority high " +
            "distribute by rowid range for service default duplicate all;")
        assertThat(p).matches("alter table customer inmemory priority high memcompress for dml;")
        assertThat(p).matches("alter table customer inmemory no memcompress no duplicate distribute for service svc;")
        assertThat(p).matches("alter table customer inmemory inmemory (customer_name);")
        assertThat(p).matches("alter table customer inmemory (customer_name, customer_id);")
        assertThat(p).matches("alter table customer inmemory memcompress for query low (name) no inmemory (customer_id);")
        assertThat(p).matches("alter table j_purchaseorder inmemory text (data);")
        assertThat(p).matches("alter table customer inmemory parallel;")
    }

    @Test
    fun matchesAlterTableStateAndImmutableClauses() {
        assertThat(p).matches("alter table t read only;")
        assertThat(p).matches("alter table t read write pctfree 10;")
        assertThat(p).matches("alter table t nologging row archival;")
        assertThat(p).matches("alter table t no row archival;")
        assertThat(p).matches("alter table t parallel for staging;")
        assertThat(p).matches("alter table t not for staging;")
        assertThat(p).matches("alter table t default collation binary_ci;")
        assertThat(p).matches("alter table t pctfree 10 annotations(add a);")
        assertThat(p).matches("alter table t no flashback archive;")
        assertThat(p).matches("alter table imm_tab no drop until 50 days idle;")
        assertThat(p).matches("alter table imm_tab no drop;")
        assertThat(p).matches("alter table imm_tab no delete until 120 days after insert;")
        assertThat(p).matches("alter table imm_tab no delete locked;")
        assertThat(p).matches("alter table imm_tab no delete until 120 days after insert no drop until 5 days idle;")
    }

    @Test
    fun matchesAlterTableTrailingEnableDisableClauses() {
        assertThat(p).matches("alter table employees enable all triggers;")
        assertThat(p).matches("alter table employees disable all triggers enable table lock;")
        assertThat(p).matches("alter table employees parallel enable all triggers;")
        assertThat(p).matches("alter table employees enable all triggers enable constraint c;")
        assertThat(p).matches("alter table employees enable container_map;")
        assertThat(p).matches("alter table employees disable containers_default;")
    }

    @Test
    fun matchesStandaloneAlterTableOperations() {
        assertThat(p).matches("alter table j_purchaseorder_new rename to j_purchaseorder;")
        assertThat(p).matches("alter table customers rename constraint cust_fname_nn to cust_firstname_nn;")
        assertThat(p).matches("alter table t shrink space compact cascade;")
        assertThat(p).matches("alter table t read only shrink space;")
        assertThat(p).matches("alter table jobs_temp move storage (initial 20k next 40k minextents 2 maxextents 20 pctincrease 0);")
        assertThat(p).matches("alter table t move online tablespace users pctfree 10 nologging parallel 2 update indexes;")
        assertThat(p).matches("alter table t move parallel storage (initial 20k) online;")
        assertThat(p).matches("alter table t move update indexes online including rows where c > 0;")
        assertThat(p).matches("alter table t move compress lob (l) store as (tablespace users);")
        assertThat(p).matches("alter table t move update indexes (ix tablespace users);")
    }

    @Test
    fun matchesAlterTableIndexOrganizedClauses() {
        assertThat(p).matches("alter table countries_demo add overflow;")
        assertThat(p).matches("alter table countries_demo add overflow tablespace users initrans 4;")
        assertThat(p).matches("alter table t add overflow tablespace users (partition tablespace users, partition);")
        assertThat(p).matches("alter table t add overflow enable all triggers;")
        assertThat(p).matches("alter table countries_demo overflow initrans 4;")
        assertThat(p).matches("alter table t overflow initrans 4 pctfree 10;")
        assertThat(p).matches("alter table t pctfree 10 overflow initrans 4;")
        assertThat(p).matches("alter table t overflow allocate extent;")
        assertThat(p).matches("alter table t pctthreshold 20;")
        assertThat(p).matches("alter table t mapping table allocate extent;")
        assertThat(p).matches("alter table t coalesce;")
        // A column named OVERFLOW is still added as a column.
        assertThat(p).matches("alter table t add overflow number;")
    }

    @Test
    fun rejectsInvalidAlterTablePropertyCombinations() {
        // ORA-14047 / ORA-23290: RENAME TO and RENAME CONSTRAINT cannot be combined.
        assertThat(p).notMatches("alter table t pctfree 10 rename to u;")
        assertThat(p).notMatches("alter table t rename to u pctfree 10;")
        assertThat(p).notMatches("alter table t rename to u enable all triggers;")
        assertThat(p).notMatches("alter table t rename constraint a to b parallel;")
        // ORA-03049 / ORA-00905: table-level ENABLE/DISABLE clauses come last.
        assertThat(p).notMatches("alter table t enable all triggers parallel;")
        assertThat(p).notMatches("alter table t enable constraint c enable row movement;")
        // ORA-10630: SHRINK must be the final operation.
        assertThat(p).notMatches("alter table t shrink space parallel;")
        assertThat(p).notMatches("alter table t shrink space read only;")
        assertThat(p).notMatches("alter table t shrink space enable all triggers;")
        // ORA-14133 / ORA-01735: MOVE cannot be combined, and ONLINE may appear only once.
        assertThat(p).notMatches("alter table t pctfree 10 move;")
        assertThat(p).notMatches("alter table t move storage (initial 20k) enable all triggers;")
        assertThat(p).notMatches("alter table t move online online;")
        // ORA-14048 / ORA-01735: ADD OVERFLOW and COALESCE are standalone.
        assertThat(p).notMatches("alter table t pctfree 10 add overflow;")
        assertThat(p).notMatches("alter table t pctfree 10 coalesce;")
        assertThat(p).notMatches("alter table t coalesce pctfree 10;")
        // ORA-00922 / ORA-01735: malformed RESULT_CACHE, INMEMORY and NO DELETE forms.
        assertThat(p).notMatches("alter table t result_cache ();")
        assertThat(p).notMatches("alter table t result_cache (mode default, mode force);")
        assertThat(p).notMatches("alter table t inmemory memcompress auto;")
        assertThat(p).notMatches("alter table t inmemory all (c);")
        assertThat(p).notMatches("alter table t inmemory priority high text (c);")
        assertThat(p).notMatches("alter table t inmemory text (c), (d);")
        assertThat(p).notMatches("alter table t no delete (locked);")
        assertThat(p).notMatches("alter table t pctfree;")
        assertThat(p).notMatches("alter table t allocate;")
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
    fun matchesAlterTableAddWithoutADatatypeWhichOracleRejectsAfterParsing() {
        assertThat(p).matches("alter table tab add (col);")
    }

    @Test
    fun matchesAddColumnWithJsonValidate() {
        assertThat(p).matches("alter table t add (j json validate cast using '{}')")
        assertThat(p).matches("alter table t add (j json sort validate '{}' not null)")
        assertThat(p).notMatches("alter table t add (j json validate cast '{}' sort)")
    }

    @Test
    fun matchesModifyDomainAssociations() {
        assertThat(p).matches("alter table t modify (c1) add domain d")
        assertThat(p).matches("alter table t modify (c1, c2) add domain s.d")
        assertThat(p).matches("alter table t modify (c1) add domain \"D\"")
        assertThat(p).matches("alter table t modify (c1) drop domain")
        assertThat(p).matches("alter table t modify (c1, c2) drop domain")
        assertThat(p).matches("alter table t modify (c1) drop domain preserve")
        assertThat(p).matches("alter table t modify (c1) drop domain preserve constraints")
        assertThat(p).matches("alter table t modify (c1 domain d)")
        assertThat(p).matches("alter table t modify (c1 number domain s.d)")
        assertThat(p).matches("alter table t modify (c1 domain d not null, c2 domain d)")
    }

    @Test
    fun matchesTableLevelDomainInAddColumns() {
        assertThat(p).matches("alter table t add (c9 number, domain d (c9))")
        assertThat(p).matches("alter table t add (c9 number, c8 number, domain s.d (c9, c8))")
        assertThat(p).matches("alter table t add domain number")
        assertThat(p).matches("alter table t add (domain number)")
    }

    @Test
    fun rejectsMalformedDomainAssociationsInAlterTable() {
        assertThat(p).notMatches("alter table t modify c1 add domain d")
        assertThat(p).notMatches("alter table t modify (c1) add domain")
        assertThat(p).notMatches("alter table t modify (c1,) add domain d")
        assertThat(p).notMatches("alter table t modify () add domain d")
        assertThat(p).notMatches("alter table t modify (c1) add domain a.b.d")
        assertThat(p).notMatches("alter table t modify (c1) drop domain constraints")
        assertThat(p).notMatches("alter table t modify (c1) add domain d not null")
        assertThat(p).notMatches("alter table t modify (c1) add domain d (c1)")
        assertThat(p).notMatches("alter table t modify (c1) add domain d add domain d")
        assertThat(p).notMatches("alter table t modify (c1) add domain d enable row movement")
        assertThat(p).notMatches("alter table t modify (c1 drop domain)")
        assertThat(p).notMatches("alter table t add domain d (c1)")
    }

    @Test
    fun matchesDatatypeDomainInAddColumns() {
        assertThat(p).matches("alter table t add c2 number domain d")
        assertThat(p).matches("alter table t add (c2 number domain d)")
        assertThat(p).matches("alter table t add (c2 domain d)")
        assertThat(p).matches("alter table t add c2 domain d")
        assertThat(p).matches("alter table t add (c2 number domain s.d not null, c3 domain \"E\")")
        assertThat(p).matches("alter table t add c2 number domain d not null")
        assertThat(p).matches("alter table t add c2 d")
        assertThat(p).notMatches("alter table t add c2 number d")
        assertThat(p).notMatches("alter table t add (c2 domain d (e))")
    }

    @Test
    fun keepsModifyDatatypeDomainBehaviour() {
        assertThat(p).matches("alter table t modify (c1 number domain d default 1)")
        assertThat(p).matches("alter table t modify c1 domain d")
        assertThat(p).notMatches("alter table t modify (c1 domain d domain d)")
        assertThat(p).notMatches("alter table t modify (c1 default 1 domain d)")
        assertThat(p).notMatches("alter table t modify (c1 not null domain d)")
    }

    @Test
    fun matchesDatatypeLessForeignKeyColumnsInAdd() {
        assertThat(p).matches("alter table t add (d, foreign key (d) references r(id))")
        assertThat(p).matches("alter table t add (d, constraint fk foreign key (d) references r(id))")
        assertThat(p).matches("alter table t add d references r(id)")
        assertThat(p).matches("alter table t add (d references r(id))")
        assertThat(p).matches("alter table t add d")
        assertThat(p).matches("alter table t add (d, e number)")
        assertThat(p).matches("alter table t add (d not null)")
        assertThat(p).notMatches("alter table t add (d collate binary_ci)")
    }

    @Test
    fun matchesModifyNestedTableReturnAs() {
        listOf(
            "alter table t modify nested table n return as value", "alter table t modify nested table n return as locator",
            "alter table t modify nested table n return value", "alter table t modify nested table s.n return as value",
            "alter table print_media modify nested table ad_textdocs_ntab return as value;",
            "alter table t modify nested table n return as value modify nested table m return as locator",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter table t modify nested table n return as", "alter table t modify nested table return as value",
            "alter table t modify nested table n return as value, modify nested table m return as locator",
            "alter table t modify nested table n return as null", "alter table t modify nested table n as value",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesModifyOpaqueType() {
        listOf(
            "alter table t1 modify opaque type x store (xmltype, clob_typ) unpacked", "alter table t modify opaque type c store (a) unpacked",
            "alter table t modify opaque type c store (sys.xmltype, s.t) unpacked",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter table t modify opaque type c store (a)", "alter table t modify opaque type c store () unpacked",
            "alter table t modify opaque type c store a unpacked", "alter table t modify opaque type c unpacked",
            "alter table t modify opaque type c store (a, b) packed", "alter table t modify opaque type c store (a,) unpacked",
            "alter table t modify opaque type c store (a) unpacked unpacked",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesModifyLobStorage() {
        listOf(
            "alter table xml_lob_tab modify lob (xmldata) (storage (maxsize 2g) cache);",
            "alter table t modify lob (c) (cache)", "alter table t modify lob (c) (nocache)", "alter table t modify lob (c) (nocache logging)",
            "alter table t modify lob (c) (cache reads nologging)", "alter table t modify lob (c) (storage (maxsize 2g))",
            "alter table t modify lob (c) (pctversion 10)", "alter table t modify lob (c) (freepools 2)",
            "alter table t modify lob (c) (rebuild freepools)", "alter table t modify lob (c) (retention)",
            "alter table t modify lob (c) (retention max)", "alter table t modify lob (c) (retention min 10)",
            "alter table t modify lob (c) (retention auto)", "alter table t modify lob (c) (retention none)",
            "alter table t modify lob (c) (deduplicate)", "alter table t modify lob (c) (keep_duplicates)",
            "alter table t modify lob (c) (compress)", "alter table t modify lob (c) (compress high)",
            "alter table t modify lob (c) (nocompress)", "alter table t modify lob (c) (encrypt)",
            "alter table t modify lob (c) (encrypt using 'aes256')", "alter table t modify lob (c) (decrypt)",
            "alter table t modify lob (c) (allocate extent (size 1m))", "alter table t modify lob (c) (deallocate unused keep 1m)",
            "alter table t modify lob (c) (shrink space cascade)", 
            "alter table t modify lob (c) (cache storage (maxsize 2g) retention shrink space)",
            "alter table t modify lob (c) (cache) modify lob (d) (nocache)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter table t modify lob (c) ()",
            "alter table t modify lob (c) (cache), modify lob (d) (nocache)", "alter table t modify lob (c) (cache) lob (d) (nocache)",
            "alter table t modify lob (c) (cache logging)",
            "alter table t modify lob (c) (retention max 10)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun parsesMultipleModifyLobItemsWhichOracleRejectsAfterParsing() {
        assertThat(p).matches("alter table t modify lob (c, d) (cache)")
        assertThat(p).notMatches("alter table t modify lob (c,) (cache)")
        assertThat(p).notMatches("alter table t modify lob () (cache)")
        assertThat(p).notMatches("alter table t modify lob (c d) (cache)")
    }

    @Test
    fun matchesModifyPartitionAttributes() {
        listOf(
            "alter table tft_tsm.t_act_trade_detail modify partition sys_p41089 shrink space;",
            "alter table t modify partition p1 shrink space compact", "alter table t modify partition p1 shrink space cascade",
            "alter table t modify partition p1 shrink space compact cascade", "alter table t modify partition for (5) shrink space",
            "alter table t modify partition p1 pctfree 5", "alter table t modify partition p1 nologging",
            "alter table t modify partition p1 logging shrink space", "alter table t modify partition p1 allocate extent",
            "alter table t modify partition p1 deallocate unused keep 1m",
            "alter table t modify partition p1 unusable local indexes",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter table t modify partition p1 shrink", "alter table t modify partition p1 shrink space pctfree 5",
            "alter table t modify partition p1 shrink space shrink space",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesModifyToPartitioned() {
        listOf(
            """alter table table_name modify partition by range (date_column)
                interval (numtodsinterval(1, 'DAY'))
                (partition values less than (to_date('2024-01-01', 'YYYY-MM-DD')));""",
            "alter table t modify partition by range (id) (partition values less than (10))",
            "alter table t modify partition by range (id) (partition p1 values less than (10), partition values less than (maxvalue))",
            "alter table t modify partition by range (id) interval (10) store in (users) (partition values less than (10))",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) online",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) online update indexes",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) update indexes (i local, j global)",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) including rows where id > 1 online",
            "alter table t modify partition by hash (id) partitions 4", "alter table t modify partition by hash (id) (partition p1, partition p2)",
            "alter table t modify partition by list (id) (partition p1 values (1), partition p2 values (default))",
            "alter table t modify partition by range (id) subpartition by hash (id) subpartitions 2 (partition p1 values less than (10))",
            "alter table t modify nonpartitioned", "alter table t modify nonpartitioned online update indexes",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter table t modify partition by range (id)", "alter table t modify partition by range (id) ()",
            "alter table t modify partition by range (id) online (partition p1 values less than (10))",
            "alter table t modify (id number) partition by range (id) (partition p1 values less than (10))",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) online online",
            "alter table t modify partition by range (id) (partition p1 values less than (10)) update indexes (i)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsModifyClauseNodes() {
        val lob = p.parse("alter table t modify lob (xmldata) (storage (maxsize 2g) cache)").getFirstDescendant(DdlGrammar.MODIFY_LOB_STORAGE_CLAUSE)
        assertThatAst(lob.tokens.map { it.originalValue }).containsExactly(
            "modify", "lob", "(", "xmldata", ")", "(", "storage", "(", "maxsize", "2", "g", ")", "cache", ")")
        assertThatAst(p.parse("alter table t modify nested table n return value")
            .getFirstDescendant(DdlGrammar.MODIFY_COLLECTION_RETRIEVAL).tokens.map { it.originalValue })
            .containsExactly("modify", "nested", "table", "n", "return", "value")
        assertThatAst(p.parse("alter table t modify opaque type x store (a, b) unpacked")
            .getFirstDescendant(DdlGrammar.MODIFY_OPAQUE_TYPE).tokens.map { it.originalValue })
            .containsExactly("modify", "opaque", "type", "x", "store", "(", "a", ",", "b", ")", "unpacked")
        val partition = p.parse("alter table t modify partition p1 pctfree 5 shrink space compact")
            .getFirstDescendant(DdlGrammar.MODIFY_TABLE_PARTITION)
        assertThatAst(partition.hasDirectChildren(DdlGrammar.INDEX_SHRINK_CLAUSE)).isTrue()
        val converted = p.parse("alter table t modify partition by range (id) interval (10) (partition values less than (10)) online")
        assertThatAst(converted.getFirstDescendant(DdlGrammar.MODIFY_TO_PARTITIONED).hasDirectChildren(DdlGrammar.PARTITION_BY_RANGE)).isTrue()
        assertThatAst(converted.getDescendants(DdlGrammar.PARTITION_INTERVAL_CLAUSE)).hasSize(1)
        assertThatAst(p.parse("alter table t modify partition p1 unusable local indexes").getDescendants(DdlGrammar.MODIFY_TABLE_PARTITION)).isEmpty()
    }
}
