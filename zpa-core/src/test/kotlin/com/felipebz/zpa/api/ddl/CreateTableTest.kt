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

class CreateTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    @Test
    fun matchesSimpleCreateTable() {
        assertThat(p).matches("create table tab (id number);")
    }

    @Test
    fun matchesMultipleColumns() {
        assertThat(p).matches("create table tab (id number, name number);")
    }

    @Test
    fun matchesSharedColumnEncryptionSpec() {
        assertThat(p).matches("create table t (c varchar2(30) encrypt)")
        assertThat(p).matches("create table t (c varchar2(30) encrypt using 'AES256')")
        assertThat(p).matches("create table t (c varchar2(30) encrypt 'NOMAC' no salt)")
        assertThat(p).matches("create table t (c varchar2(30) encrypt using 'AES256' 'NOMAC' no salt)")
        assertThat(p).matches("create table t (c varchar2(30) encrypt identified by password)")
    }

    @Test
    fun rejectsMalformedSharedColumnEncryptionSpec() {
        assertThat(p).notMatches("create table t (c varchar2(30) encrypt using)")
        assertThat(p).notMatches("create table t (c varchar2(30) encrypt no)")
        assertThat(p).notMatches("create table t (c varchar2(30) decrypt)")
    }

    @Test
    fun matchesSharedOutOfLineRefConstraints() {
        assertThat(p).matches("create table t (dept ref obj_type, scope for (dept) is schema_name.offices)")
        assertThat(p).matches("create table t (dept ref obj_type, ref(dept) with rowid)")
        assertThat(p).matches("create table t (scope number, ref number)")
    }

    @Test
    fun rejectsIncompleteSharedRefConstraints() {
        assertThat(p).notMatches("create table t (dept ref obj_type, scope for (dept))")
        assertThat(p).notMatches("create table t (dept ref obj_type, ref(dept))")
    }

    @Test
    fun matchesObjectTables() {
        assertThat(p).matches("create table sch.tab of sch.obj_type;")
        assertThat(p).matches("create table t of obj_type substitutable at all levels;")
        assertThat(p).matches("create table t of obj_type not substitutable at all levels;")
        assertThat(p).matches(
            "create global temporary table t of obj_type " +
                "on commit preserve rows object identifier is system generated;"
        )
        assertThat(p).matches("create table t of obj_type object identifier is system generated;")
        assertThat(p).matches(
            "create table tab of obj_type (id primary key) object identifier is primary key;"
        )
    }

    @Test
    fun matchesNestedTableStorage() {
        assertThat(p).matches(
            "create table tab (values_list value_list_type) " +
                "nested table values_list store as t_values;"
        )
        assertThat(p).matches(
            "create table tab (warnings warning_list) " +
                "nested table warnings store as tab_warnings;"
        )
        assertThat(p).matches(
            "create table tab (warnings warning_list, tags tag_list) " +
                "nested table warnings store as tab_warnings return as value " +
                "nested table tags store as tab_tags return as locator;"
        )
        assertThat(p).matches(
            "create table tab of tab_type " +
                "nested table warnings store as tab_warnings return as locator;"
        )
    }

    @Test
    fun matchesRecursiveNestedTableStorage() {
        assertThat(p).matches(
            "create table tab (warnings warning_list) " +
                "nested table warnings store as outer_warnings " +
                "(nested table column_value store as inner_warnings);"
        )
    }

    @Test
    fun rejectsMalformedObjectAndNestedTableClauses() {
        assertThat(p).notMatches("create table tab of;")
        assertThat(p).notMatches(
            "create table tab of obj_type is of type (only other_type);"
        )
        assertThat(p).notMatches(
            "create table tab of obj_type element is of type (only other_type);"
        )
        assertThat(p).notMatches(
            "create table tab of obj_type object foo is system generated;"
        )
        assertThat(p).notMatches(
            "create table tab of obj_type object whatever is primary key;"
        )
        assertThat(p).notMatches(
            "create table tab of XMLTYPE " +
                "XMLSCHEMA 'http://example.test/schema.xsd' ELEMENT 'Tab';"
        )
        assertThat(p).notMatches(
            "create global temporary table t of obj_type " +
                "object identifier is system generated on commit preserve rows;"
        )
        assertThat(p).notMatches("create table tab (values_list value_list_type) nested table values_list;")
        assertThat(p).notMatches(
            "create table tab (values_list value_list_type) " +
                "nested table values_list store as other_schema.t_values;"
        )
        assertThat(p).notMatches(
            "create table tab (warnings warning_list) nested table warnings store as;"
        )
        assertThat(p).notMatches(
            "create table tab (warnings warning_list) " +
                "nested table warnings store as tab_warnings return;"
        )
        assertThat(p).notMatches(
            "create table tab (warnings warning_list) " +
                "nested table warnings store as outer_warnings " +
                "(nested table column_value store as);"
        )
    }

    @Test
    fun matchesTableWithSchema() {
        assertThat(p).matches("create table sch.tab (id number);")
    }

    @Test
    fun matchesTemporaryTable() {
        assertThat(p).matches("create global temporary table tab (id number);")
    }

    @Test
    fun matchesTemporaryTableWithTablespace() {
        assertThat(p).matches("create global temporary table tab (id number) tablespace table_space;")
    }

    @Test
    fun matchesTemporaryTableOnCommitDeleteRows() {
        assertThat(p).matches("create global temporary table tab (id number) on commit delete rows;")
    }

    @Test
    fun matchesTemporaryTableOnCommitPreserveRows() {
        assertThat(p).matches("create global temporary table tab (id number) on commit preserve rows;")
    }

    @Test
    fun matchesCreateTableWithOutOfLineConstraint() {
        assertThat(p).matches("create table tab (id number, constraint pk primary key(id));")
    }

    @Test
    fun matchesSupplementalLoggingProperties() {
        assertThat(p).matches("create table tab (a number, supplemental log data (all) columns);")
        assertThat(p).matches(
            "create table tab (a number, b number, supplemental log data (primary key, unique, foreign key, all) columns)"
        )
        assertThat(p).matches(
            "create table tab (a number, b number, supplemental log group g1 (a no log, b) always)"
        )
        assertThat(p).matches(
            "create table tab (a number, constraint pk primary key (a) using index enable, " +
                "supplemental log data (all) columns);"
        )
        assertThat(p).matches(
            "create table tab (a number, primary key (a) using index tablespace users enable, " +
                "supplemental log data (all) columns) segment creation immediate;"
        )
        assertThat(p).matches(
            "create table tab (a number, b number, primary key (a), supplemental log data (all) columns, " +
                "supplemental log group g1 (b))"
        )
        assertThat(p).matches("create table tab (supplemental number)")
    }

    @Test
    fun rejectsMalformedSupplementalLoggingProperties() {
        assertThat(p).notMatches("create table tab (a number supplemental log data (all) columns)")
        assertThat(p).notMatches("create table tab (a number, supplemental log data (all) columns b number)")
        assertThat(p).notMatches("create table tab (a number, constraint supplemental log data (all) columns)")
        assertThat(p).notMatches("create table tab (a number, supplemental log data () columns)")
        assertThat(p).notMatches("create table tab (a number, supplemental log data (all,) columns)")
        assertThat(p).notMatches("create table tab (a number, supplemental log data (all))")
        assertThat(p).notMatches("create table tab (a number, supplemental log group g1 () )")
        assertThat(p).notMatches("create table tab (a number, supplemental log group g1 (a) no log)")
    }

    @Test
    fun matchesCreateTableWithPrimaryKeyUsingIndex() {
        assertThat(p).matches("create table tab (id number, constraint tab_pk primary key (id) using index);")
    }

    @Test
    fun matchesCreateTableWithUniqueUsingIndex() {
        assertThat(p).matches("create table tab (a number, b number, constraint tab_uk unique (a, b) using index);")
    }

    @Test
    fun rejectsUsingIndexOnNotNullConstraint() {
        assertThat(p).notMatches("create table tab (foo number not null using index);")
    }

    @Test
    fun matchesIndexOrganizedTables() {
        assertThat(p).matches("create table t (id number primary key) organization index;")
        assertThat(p).matches("create table t (id number primary key) organization index nologging initrans 10;")
        assertThat(p).matches("create table t (id number primary key) organization index nologging initrans 100 overflow nologging initrans 100;")
    }

    @Test
    fun matchesIndexOrganizedTableOptions() {
        assertThat(p).matches("create table t (id number primary key) organization index mapping table;")
        assertThat(p).matches("create table t (id number primary key) organization index nomapping;")
        assertThat(p).matches("create table t (id number primary key) organization index compress 1;")
        assertThat(p).matches("create table t (id number primary key) organization index compress advanced low;")
        assertThat(p).matches("create table t (id number primary key) organization index tablespace users;")
        assertThat(p).matches("create table t (id number primary key) organization index overflow tablespace users;")
        assertThat(p).matches(
            "create table countries_demo (" +
                "country_id char(2) not null, " +
                "country_name varchar2(40), " +
                "constraint countries_demo_pk primary key (country_id)" +
                ") organization index including country_name pctthreshold 2 storage (initial 4k) " +
                "overflow storage (initial 4k);"
        )
    }

    @Test
    fun matchesMutuallyExclusiveIndexOrganizedOverflowForms() {
        assertThat(p).matches("create table t (id number primary key) organization index overflow;")
        assertThat(p).matches("create table t (id number primary key) organization index including id overflow;")
        assertThat(p).matches(
            "create table t (id number primary key, value varchar2(100)) " +
                "organization index including value pctthreshold 20 storage (initial 4k) " +
                "overflow storage (initial 4k);"
        )
    }

    @Test
    fun rejectsDuplicateIndexOrganizedOverflowClauses() {
        assertThat(p).notMatches("create table t (id number primary key) organization index overflow overflow;")
        assertThat(p).notMatches("create table t (id number primary key) organization index including id overflow overflow;")
        assertThat(p).notMatches(
            "create table t (id number primary key) organization index including id " +
                "pctthreshold 20 overflow overflow;"
        )
    }

    @Test
    fun rejectsMalformedIndexOrganizedTableClauses() {
        assertThat(p).notMatches("create table t (id number primary key) organization;")
        assertThat(p).notMatches("create table t (id number primary key) organization index pctthreshold;")
        assertThat(p).notMatches("create table t (id number primary key) organization index including id;")
        assertThat(p).notMatches("create table t (id number primary key) organization index overflow initrans;")
        assertThat(p).notMatches("create table t (id number primary key) organization index overflow tablespace;")
        assertThat(p).notMatches("create table t (id number primary key) organization index overflow nologging pctthreshold 20;")
    }

    @Test
    fun matchesPartitionByRangeMulti() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id1, column_id2) (partition patition_id values less than (column_id));")
    }

    @Test
    fun matchesPartitionByRange_RVC_I() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (column_id));")
    }

    @Test
    fun matchesPartitionByRange_RVC_IE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition values less than (column_id));")
    }

    @Test
    fun matchesPartitionByRange_RVC_IM() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (column_id1, column_id2));")
    }

    @Test
    fun matchesPartitionByRange_RV_MV() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_PCTFREE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) pctfree 100);")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_PCTUSED() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) pctused 100);")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_INITRANS() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) initrans 100);")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_K() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (initial 100 k));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_M() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (initial 100 m));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_G() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (initial 100 g));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_T() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (initial 100 t));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_P() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (initial 100 p));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_NEXT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (next 100 k));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_MINE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (minextents 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_MAXE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (maxextents 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_MAXE_U() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (maxextents unlimited));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_PCTI() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (pctincrease 100) );")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_FLS() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (freelists 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_FL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (freelist groups 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_OP() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (optimal 100 k));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_OP_NULL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (optimal null));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_BP_K() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (buffer_pool keep));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_BP_R() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (buffer_pool recycle));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_PAC_SC_ISC_BP_D() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) storage (buffer_pool default));")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_TB() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) pctfree 100 tablespace tablespace_id1);")
    }

    @Test
    fun matchesPartitionByRange_TPD_SAC_LOG() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) pctfree 100 tablespace tablespace_id1 logging);")
    }

    @Test
    fun matchesPartitionByRange_TPD_TC() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionByRange_TPD_TNC() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) nocompress);")
    }

    @Test
    fun matchesPartitionByRange_TPD_KC() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) mapping table);")
    }

    @Test
    fun matchesPartitionByRange_TPD_NP() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) nomapping);")
    }

    @Test
    fun matchesPartitionByRange_TPD_OF() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) overflow pctfree 100 tablespace tablespace_id1 logging);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_TS() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (tablespace tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_TSM() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (tablespace tablespace_id1 tablespace tablespace_id2));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_SNE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (enable storage in row));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_SND() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (disable storage in row));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_SC() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (storage (initial 100 k)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_C() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (chunk 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_PCT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (pctversion 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_R() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (retention));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_FP() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (freepools 100));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_CACHE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (cache));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_NCACHE() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (nocache));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_NCACHE_LOG() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (nocache logging));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_CACHE_R() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (cache reads));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPARSL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as segment_id (tablespace tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPARSI() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as segment_id);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_LPAR() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id store as lob segment_id);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCCET_DT_LPAR() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id element is of type (only number) store as lob segment_id (tablespace tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCCET_DT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id element is of type (only number) store as lob segment_id);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCCT_DT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id is of type (only number) store as lob segment_id);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCC_DT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id is of (only number) store as lob segment_id);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCCS_DT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id substitutable at all levels);")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_VARRAY_SCCNS_DT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) varray array_id not substitutable at all levels);")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_STORE_TBL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) subpartitions 1 store in (tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_STORE_TBLS() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) subpartitions 1 store in (tablespace_id1, tablespace_id2));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value')));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCLM() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value', null)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCN() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values (null)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCD() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values (default)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_TBL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') tablespace tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_OVFL_TBL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') overflow tablespace tablespace_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_OVFL() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') overflow));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_LOBST() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') lob (lob_id) store as segment_id (tablespace tablespace_id)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_LOBS() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') lob (lob_id) store as segment_id));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_LOBT() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') lob (lob_id) store as (tablespace tablespace_id)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_LOBTM() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') lob (lob_id1) store as (tablespace tablespace_id1) lob (lob_id2) store as (tablespace tablespace_id2)));")
    }

    @Test
    fun matchesPartitionByRange_TPD_PLS_SS_IDENT_LVCL_PSC_VARRAY() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) (subpartition subpartition_id values ('value') varray array_id store as lob segment_id));")
    }

    @Test
    fun matchesPartitionByHash_IHP_PSC() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) (partition partition_id tablespace tablespace_id);")
    }

    @Test
    fun matchesPartitionByHashM_IHP_PSC() {
        assertThat(p).matches("create table table_id (id1 number, id2 varchar) partition by hash (column_id) (partition partition_id tablespace tablespace_id);")
    }

    @Test
    fun matchesPartitionByHash_IHPM_PSC() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) (partition partition_id1 tablespace tablespace_id1, partition partition_id2 tablespace tablespace_id2);")
    }

    @Test
    fun matchesPartitionByHash_HPBQ() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) partitions 1;")
    }

    @Test
    fun matchesPartitionByHash_HPBQ_STORE() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) partitions 1 store in (tablespace_id);")
    }

    @Test
    fun matchesPartitionByHash_HPBQ_STOREM() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) partitions 1 store in (tablespace_id1, tablespace_id2);")
    }

    @Test
    fun matchesPartitionByHash_HPBQ_STORE_OFLW() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) partitions 1 store in (tablespace_id) overflow store in (tablespace_id);")
    }

    @Test
    fun matchesPartitionByHash_HPBQ_STORE_OFLWM() {
        assertThat(p).matches("create table table_id (id number) partition by hash (column_id) partitions 1 store in (tablespace_id) overflow store in (tablespace_id1, tablespace_id2);")
    }

    @Test
    fun matchesPartitionByHash_PBL_LVC() {
        assertThat(p).matches("create table table_id (id number) partition by list (column_id) (partition partition_id values (default) compress);")
    }

    @Test
    fun matchesPartitionByHash_PBLN_LVC() {
        assertThat(p).matches("create table table_id (id number) partition by list (column_id) (partition values (default) compress);")
    }

    @Test
    fun matchesPartitionByHash_PBLM_LVC() {
        assertThat(p).matches("create table table_id (id number) partition by list (column_id) (partition partition_id1 values (default) compress, partition partition_id2 values (null) nocompress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_LVC_PSC_IDENT() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id values (default) tablespace tablespace_id) (partition partition_id values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_LVC_PSC() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id values (default) tablespace tablespace_id) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_LVC() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id values (default)) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_LVCM() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id1 values (default), subpartition subpartition_id2 values (null)) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template 1 (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_HSQ() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBL_ST_LVC_PSC_MULT() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by list (column_id) subpartition template (subpartition subpartition_id values (default) tablespace tablespace_id) (partition partition_id1 values less than (maxvalue) compress, partition partition_id2 values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBH_SUB_STORE() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by hash (column_id) subpartitions 1 store in (tablespace_id) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBH_SUB_STOREM() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by hash (column_id) subpartitions 1 store in (tablespace_id1, tablespace_id2) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBH_ST() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by hash (column_id) subpartition template 1 (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesPartitionComposite_SBH() {
        assertThat(p).matches("create table table_id (id number) partition by range (column_id) subpartition by hash (column_id) (partition values less than (maxvalue) compress);")
    }

    @Test
    fun matchesColumnDefaultOnNull() {
        assertThat(p).matches("create table table_id (id number, name varchar2(100) default on null 'Default String');")
    }

    @Test
    fun matchesColumnDefaultOnNullWithExpression() {
        assertThat(p).matches("create table table_id (id number, created_date date default on null sysdate);")
    }

    @Test
    fun matchesColumnDefault() {
        assertThat(p).matches("create table table_id (id number, name varchar2(100) default 'Default String');")
    }

    @Test
    fun matchesColumnDefaultOnNullForInsertOnly() {
        assertThat(p).matches("create table table_id (id number, name varchar2(100) default on null for insert only 'Default String');")
    }

    @Test
    fun matchesColumnDefaultOnNullForInsertOnlyWithExpression() {
        assertThat(p).matches("create table table_id (id number, created_date date default on null for insert only sysdate);")
    }

    @Test
    fun matchesColumnDefaultOnNullForInsertAndUpdate() {
        assertThat(p).matches("create table table_id (id number, name varchar2(100) default on null for insert and update 'Default String');")
    }

    @Test
    fun matchesColumnDefaultOnNullForInsertAndUpdateWithExpression() {
        assertThat(p).matches("create table table_id (id number, created_date date default on null for insert and update sysdate);")
    }

    @Test
    fun matchesCreateTableAsSelect() {
        assertThat(p).matches("create table tab_bkp as select * from tab;")
        assertThat(p).matches("create global temporary table tab_tmp as select * from tab where 1 = 0;")
    }

    @Test
    fun doesNotMatchCreateTableAsWithoutASubquery() {
        assertThat(p).notMatches("create table tab_bkp as;")
    }

    @Test
    fun matchesColumnAndTableAnnotations() {
        assertThat(p).matches("create table t (c number annotations(Display 'Value', Hidden))")
        assertThat(p).matches("create table t (c number) annotations(Display 'Table')")
        assertThat(p).matches("create table t (c number annotations(Display 'Column')) annotations(Display 'Table')")
        assertThat(p).matches("create table t (c number) annotations(add Hidden)")
        assertThat(p).matches("create table t (c number) annotations(add if not exists Foo 'x')")
        assertThat(p).matches("create table t (c number) annotations(Operations '[\"Sort\", \"Group\"]', Hidden)")
        assertThat(p).matches("create table t (c number) annotations(Operations 'Sort', Operations 'Group', Hidden)")
        assertThat(p).matches("create table t (id number(5) annotations(Identity, Display 'ID', \"Group\" 'Emp_Info'))")
        assertThat(p).matches("create table t (c number default 1 not null annotations(Display 'C'))")
        assertThat(p).matches("create table t (c number) tablespace users annotations(Display 'T')")
        assertThat(p).matches("create table t (c number) partition by hash (c) partitions 2 annotations(Display 'T')")
        assertThat(p).matches(
            "create global temporary table t (c number) on commit preserve rows annotations(Display 'T')")
        assertThat(p).matches("create table t annotations(Display 'T') as select 1 c from dual")
        assertThat(p).matches("create table t of person_t annotations(Display 'O')")
    }

    @Test
    fun rejectsMalformedAnnotations() {
        assertThat(p).notMatches("create table t (c number) annotations")
        assertThat(p).notMatches("create table t (c number) annotations()")
        assertThat(p).notMatches("create table t (c number) annotations(Display,)")
        assertThat(p).notMatches("create table t (c number) annotations(add)")
        assertThat(p).notMatches("create table t (c number annotations())")
        // Oracle 26 requires the column annotations after DEFAULT and inline constraints (ORA-03099/ORA-03076).
        assertThat(p).notMatches("create table t (c number annotations(Display 'C') not null)")
        assertThat(p).notMatches("create table t (c number annotations(Display 'C') default 1)")
        // Annotations may not precede ON COMMIT (ORA-00922).
        assertThat(p).notMatches(
            "create global temporary table t (c number) annotations(Display 'T') on commit preserve rows")
    }

    @Test
    fun rejectsAlterOnlyAnnotationDirectives() {
        // Oracle 26 raises ORA-11555/ORA-11556 at the directive, before diagnosing trailing tokens.
        assertThat(p).notMatches("create table t (c number) annotations(drop Foo)")
        assertThat(p).notMatches("create table t (c number) annotations(drop if exists Foo)")
        assertThat(p).notMatches("create table t (c number) annotations(replace Foo 'x')")
        assertThat(p).notMatches("create table t (c number) annotations(add or replace Foo 'x')")
        assertThat(p).notMatches("create table t (c number annotations(drop Foo))")
        assertThat(p).notMatches("create table t (c number annotations(replace Foo 'x'))")
    }

    @Test
    fun matchesTableAnnotationsAroundPartitioningAndTablespace() {
        // Orders and repetition executed by Oracle 26ai.
        assertThat(p).matches("create table t (c number) annotations(Display 'T') tablespace users")
        assertThat(p).matches("create table t (c number) annotations(Display 'T') partition by hash (c) partitions 2")
        assertThat(p).matches(
            "create table t (c number) annotations(Display 'T') partition by hash (c) partitions 2 tablespace users")
        assertThat(p).matches(
            "create table t (c number) partition by hash (c) partitions 2 annotations(Display 'T') tablespace users")
        assertThat(p).matches(
            "create table t (c number) partition by hash (c) partitions 2 tablespace users annotations(Display 'T')")
        assertThat(p).matches("create table t (c number) annotations(A '1') tablespace users annotations(B '2')")
        assertThat(p).matches("create table t (c number) annotations(A '1') annotations(B '2')")
        assertThat(p).matches(
            "create table t (c number) annotations(A '1') partition by hash (c) partitions 2 annotations(B '2')")
        assertThat(p).matches("create table t annotations(Display 'T') tablespace users as select 1 c from dual")
        assertThat(p).matches("create table t tablespace users annotations(Display 'T') as select 1 c from dual")
        assertThat(p).matches("create table t (c number primary key) organization index annotations(Display 'T')")
        assertThat(p).matches(
            "create table t (c number primary key) organization index tablespace users annotations(Display 'T')")
    }

    @Test
    fun rejectsMisplacedTableAnnotations() {
        // ORA-64303 before ORGANIZATION INDEX; ORA-03048 after the defining query.
        assertThat(p).notMatches("create table t (c number primary key) annotations(Display 'T') organization index")
        assertThat(p).notMatches("create table t as select 1 c from dual annotations(Display 'T')")
        // ORA-00922: annotations may not precede ON COMMIT, even after partitioning or TABLESPACE.
        assertThat(p).notMatches(
            "create global temporary table t (c number) tablespace users annotations(Display 'T') on commit preserve rows")
        assertThat(p).matches(
            "create global temporary table t (c number) on commit preserve rows annotations(A '1') annotations(B '2')")
    }

    @Test
    fun matchesTableSegmentAttributesAndParallelClause() {
        assertThat(p).matches("create table t (c number) tablespace users storage (initial 8m);")
        assertThat(p).matches("create table t (c number) storage (initial 8m maxsize 1g);")
        assertThat(p).matches("create table t (c number) storage (initial 8m) tablespace users pctfree 10 nologging;")
        assertThat(p).matches("create table t (c number) pctused 40 initrans 2 maxtrans 255 logging;")
        assertThat(p).matches("create table t (c number) parallel 5;")
        assertThat(p).matches("create table t (c number) noparallel;")
        // Oracle 26 interleaves PARALLEL, segment attributes and annotations in any order.
        assertThat(p).matches("create table t (c number) parallel 2 tablespace users;")
        assertThat(p).matches("create table t (c number) tablespace users parallel 2 annotations(Display 'T');")
        assertThat(p).matches("create table t (c number) annotations(Display 'T') storage (initial 8m) parallel;")
    }

    @Test
    fun matchesTablePropertiesAroundPartitioningAndColumnProperties() {
        assertThat(p).matches("create table t (c number) nologging pctfree 5 partition by hash (c) partitions 2;")
        assertThat(p).matches("create table t (c number) nologging parallel 16 partition by hash (c) partitions 2;")
        assertThat(p).matches("create table t (c number) partition by hash (c) partitions 2 storage (initial 8m) parallel 4;")
        assertThat(p).matches(
            "create table t (c number) storage (initial 100k next 50k) logging " +
                "partition by range (c) (partition p1 values less than (10) tablespace tsa storage (initial 20k));")
        assertThat(p).matches("create table t (c number, l clob) tablespace users lob (l) store as (tablespace users);")
        assertThat(p).matches("create table t (c number, l clob) lob (l) store as (tablespace users) tablespace users parallel;")
        assertThat(p).matches("create table t (c number, l clob) parallel lob (l) store as (tablespace users);")
        assertThat(p).matches("create table t (c number primary key) organization index parallel;")
    }

    @Test
    fun matchesDeferredSegmentCreation() {
        assertThat(p).matches("create table t (c number, d varchar2(20)) segment creation deferred;")
        assertThat(p).matches("create table t (c number) segment creation immediate tablespace users parallel;")
        assertThat(p).matches("create table t (c number) segment creation deferred partition by hash (c) partitions 2;")
        assertThat(p).matches(
            "create table t (c number, l clob) segment creation deferred lob (l) store as (tablespace users) tablespace users;")
        assertThat(p).matches("create table t (c number primary key) segment creation deferred organization index;")
    }

    @Test
    fun matchesPartitionSegmentCreationAroundPhysicalProperties() {
        assertThat(p).matches(
            "create table t (c number) partition by range (c) " +
                "(partition p1 values less than (10) segment creation immediate, " +
                "partition p2 values less than (maxvalue) segment creation deferred);"
        )
        assertThat(p).matches(
            "create table t (c number) partition by list (c) " +
                "(partition p1 values (1) segment creation immediate, partition p2 values (default) segment creation deferred);"
        )
        assertThat(p).matches(
            "create table t (c number) partition by range (c) " +
                "(partition p1 values less than (10) pctfree 10 segment creation immediate " +
                "storage (initial 64k) tablespace users logging nocompress);"
        )
        assertThat(p).matches(
            "create table t (c number) partition by range (c) " +
                "(partition p1 values less than (10) compress segment creation deferred logging pctfree 10);"
        )
        assertThat(p).matches(
            "create table t (c number) partition by list (c) " +
                "(partition p1 values (default) segment creation immediate " +
                "pctfree 10 nocompress logging storage (initial 64k) tablespace users);"
        )
        assertThat(p).matches(
            "create table t (c number, l clob) partition by range (c) " +
                "(partition p1 values less than (10) lob (l) store as (tablespace users) " +
                "segment creation deferred tablespace users);"
        )
    }

    @Test
    fun matchesPartitionSegmentCreationAroundColumnAndOverflowProperties() {
        assertThat(p).matches(
            "create table t (c number, l clob, m clob) partition by range (c) " +
                "(partition p1 values less than (10) segment creation immediate " +
                "lob (l) store as (tablespace users) segment creation deferred " +
                "lob (m) store as (tablespace users) segment creation immediate);"
        )
        assertThat(p).matches(
            "create table t (c number, v sys.odcinumberlist, l clob) " +
                "varray v store as lob v_lob partition by range (c) " +
                "(partition p1 values less than (10) varray v store as lob v_p1 " +
                "segment creation immediate lob (l) store as (tablespace users));"
        )
        assertThat(p).matches(
            "create table t (c number, v sys.odcinumberlist) " +
                "varray v store as lob v_lob partition by range (c) " +
                "(partition p1 values less than (10) segment creation immediate varray v store as lob v_p1);"
        )
        assertThat(p).matches(
            "create table t (c number, n nt_type) nested table n store as nstore " +
                "partition by range (c) (partition p1 values less than (10) " +
                "nested table n store as n_p1 segment creation deferred);"
        )
        assertThat(p).matches(
            "create table t (c number, n nt_type) nested table n store as nstore " +
                "partition by range (c) (partition p1 values less than (10) " +
                "segment creation immediate nested table n store as n_p1);"
        )
        assertThat(p).matches(
            "create table t (c number primary key, d varchar2(2000)) " +
                "organization index including c overflow partition by range (c) " +
                "(partition p1 values less than (10) segment creation immediate overflow tablespace users);"
        )
        assertThat(p).matches(
            "create table t (c number primary key, d varchar2(2000)) " +
                "organization index including c overflow partition by range (c) " +
                "(partition p1 values less than (10) overflow tablespace users segment creation immediate);"
        )
        assertThat(p).matches(
            "create table t (c number primary key, d varchar2(2000)) " +
                "organization index including c overflow partition by range (c) " +
                "(partition p1 values less than (10) overflow segment creation immediate tablespace users);"
        )
    }

    @Test
    fun matchesCompositeAndSubpartitionSegmentCreation() {
        assertThat(p).matches(
            "create table t (c number, d number) partition by range (c) subpartition by list (d) " +
                "(partition p1 values less than (10) segment creation deferred " +
                "(subpartition s1 values (default) tablespace users segment creation immediate));"
        )
        assertThat(p).matches(
            "create table t (c number, d number) partition by range (c) subpartition by hash (d) " +
                "subpartition template (subpartition s1 segment creation immediate) " +
                "(partition p1 values less than (10) segment creation deferred);"
        )
        assertThat(p).matches(
            "create table t (c number) partition by hash (c) " +
                "(partition p1 segment creation immediate, partition p2 segment creation deferred);"
        )
    }

    @Test
    fun rejectsIncompleteOrMisplacedPartitionSegmentCreation() {
        assertThat(p).notMatches(
            "create table t (c number) partition by range (c) (partition p1 values less than (10) segment creation);"
        )
        assertThat(p).notMatches(
            "create table t (c number) partition by list (c) (partition p1 values (default) segment creation unknown);"
        )
        assertThat(p).notMatches(
            "create table t (c number, d number) partition by range (c) subpartition by list (d) " +
                "(partition p1 values less than (10) (subpartition s1 values (default)) segment creation immediate);"
        )
        assertThat(p).notMatches(
            "create table t (c number, d number) partition by range (c) subpartition by list (d) " +
                "(partition p1 values less than (10) (subpartition s1 values (default) segment creation));"
        )
        // Preserve the previous cardinality for compression even when SEGMENT CREATION intervenes.
        assertThat(p).notMatches(
            "create table t (c number) partition by range (c) " +
                "(partition p1 values less than (10) compress segment creation immediate nocompress);"
        )
        // Oracle reports ORA-12990 for duplicate complete clauses after parsing.
        assertThat(p).matches(
            "create table t (c number) partition by range (c) " +
                "(partition p1 values less than (10) segment creation deferred segment creation immediate);"
        )
    }

    @Test
    fun matchesTablePropertiesInCreateTableAsSelect() {
        assertThat(p).matches("create table t parallel as select * from employees where department_id = 80;")
        assertThat(p).matches("create table t initrans 10 as select sysdate from dual;")
        assertThat(p).matches("create table t parallel nologging as select 1 c from dual;")
        assertThat(p).matches("create table t segment creation deferred as select 1 c from dual;")
        assertThat(p).matches(
            "create table t nologging parallel 16 partition by hash (c) partitions 512 as select * from source_table;")
    }

    @Test
    fun matchesTablePropertiesAfterOnCommit() {
        assertThat(p).matches(
            "create global temporary table t (c number) on commit preserve rows tablespace temp annotations(A) parallel;")
        assertThat(p).matches("create global temporary table t (c number) tablespace temp on commit delete rows;")
    }

    @Test
    fun rejectsMisplacedOrIncompleteTableProperties() {
        // ORA-00922: SEGMENT CREATION must precede every other physical property and may not repeat.
        assertThat(p).notMatches("create table t (c number) tablespace users segment creation immediate;")
        assertThat(p).notMatches("create table t (c number) parallel segment creation deferred;")
        assertThat(p).notMatches("create table t (c number) segment creation immediate segment creation deferred;")
        assertThat(p).notMatches("create table t (c number) partition by hash (c) partitions 2 segment creation deferred;")
        assertThat(p).notMatches("create table t (c number, l clob) lob (l) store as (tablespace users) segment creation deferred;")
        // ORA-64303: physical properties may not precede ORGANIZATION INDEX.
        assertThat(p).notMatches("create table t (c number primary key) pctfree 10 organization index;")
        assertThat(p).notMatches("create table t (c number primary key) parallel organization index;")
        // ORA-00922: only partitioning and TABLESPACE may precede ON COMMIT.
        assertThat(p).notMatches("create global temporary table t (c number) parallel on commit preserve rows;")
        assertThat(p).notMatches("create global temporary table t (c number) pctfree 10 on commit preserve rows;")
        assertThat(p).notMatches("create global temporary table t (c number) segment creation deferred on commit preserve rows;")
        // ORA-14301: table-level column properties may not follow partitioning.
        assertThat(p).notMatches(
            "create table t (c number, l clob) partition by hash (c) partitions 2 lob (l) store as (tablespace users);")
        // ORA-00922: FILESYSTEM_LIKE_LOGGING is not a table logging option.
        assertThat(p).notMatches("create table t (c number) filesystem_like_logging;")
        assertThat(p).notMatches("create table t (c number) segment creation;")
        assertThat(p).notMatches("create table t (c number) pctfree;")
        assertThat(p).notMatches("create table t (c number) maxtrans;")
        assertThat(p).notMatches("create table t (c number) storage ();")
    }

    @Test
    fun matchesRowMovementAmongTableProperties() {
        assertThat(p).matches("create table t (c number) enable row movement partition by hash (c) partitions 2;")
        assertThat(p).matches("create table t (c number) partition by hash (c) partitions 2 enable row movement parallel;")
        assertThat(p).matches("create table t (c number) disable row movement tablespace users;")
        assertThat(p).matches(
            "create table sales (c number) storage (initial 100k next 50k) logging " +
                "partition by range (c) (partition p1 values less than (10) tablespace tsa) enable row movement;")
        assertThat(p).notMatches("create table t (c number) enable row;")
    }
}
