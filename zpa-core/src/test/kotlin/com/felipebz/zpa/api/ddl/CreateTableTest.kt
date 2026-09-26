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
}
