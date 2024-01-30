/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

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
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (enable storage in now));")
    }

    @Test
    fun matchesPartitionByRange_TPD_LSC_LPAR_SND() {
        assertThat(p).matches("create global temporary table table_id (id number) partition by range (column_id) (partition patition_id values less than (maxvalue) lob (lob_id) store as (disable storage in now));")
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

}
