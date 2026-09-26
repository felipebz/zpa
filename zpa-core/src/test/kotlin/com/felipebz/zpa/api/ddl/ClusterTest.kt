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

class ClusterTest : RuleTest() {

    @Test
    fun matchesCreateCluster() {
        setRootRule(DdlGrammar.CREATE_CLUSTER)
        assertThat(p).matches("create cluster personnel (department number(4)) size 512 storage (initial 100k next 50k);")
        assertThat(p).matches("create cluster if not exists hr.c (a number sort, b varchar2(3) collate binary)")
        assertThat(p).matches("create cluster language (cust_language varchar2(3)) size 512 hashkeys 10")
        assertThat(p).matches("create cluster address (postal_code number, country_id char(2)) " +
            "hashkeys 20 hash is mod(postal_code + country_id, 101);")
        assertThat(p).matches("create cluster c (a number(6)) size 512 single table hashkeys 100")
        assertThat(p).matches("create cluster c (a number) hash is a hashkeys 10")
        assertThat(p).matches("create cluster c (a number) pctfree 10 size 1k pctused 40 index initrans 2")
        assertThat(p).matches("create cluster c (a number) parallel 2 size 512 nocache rowdependencies")
        assertThat(p).matches("create cluster sales (amount_sold number, prod_id number) hashkeys 100000 " +
            "hash is (amount_sold * 10 + prod_id) size 300 tablespace example " +
            "partition by range (amount_sold) (partition p1 values less than (2001), " +
            "partition values less than (maxvalue));")
    }

    @Test
    fun matchesAlterCluster() {
        setRootRule(DdlGrammar.ALTER_CLUSTER)
        assertThat(p).matches("alter cluster personnel size 1024 cache;")
        assertThat(p).matches("alter cluster language deallocate unused keep 30 k")
        assertThat(p).matches("alter cluster if exists hr.c parallel 2 cache")
        assertThat(p).matches("alter cluster c modify partition p1 allocate extent (size 1m)")
        assertThat(p).matches("alter cluster c allocate extent")
        assertThat(p).matches("alter cluster c storage (next 1m maxextents 5) pctfree 5")
    }

    @Test
    fun matchesCreateTableInCluster() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table dept_10 cluster personnel (department_id) " +
            "as select * from employees where department_id = 10;")
        assertThat(p).matches("create table dept (deptno number(3) primary key) cluster emp_dept (deptno);")
        assertThat(p).matches("create table t (a number, b number, c clob) cluster hr.c (a, b) lob (c) store as s")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_CLUSTER)
        // ORA-00906: the key column list is required.
        assertThat(p).notMatches("create cluster c")
        // ORA-00922: LOGGING and SHARING are not cluster options.
        assertThat(p).notMatches("create cluster c (a number) logging")
        assertThat(p).notMatches("create cluster c (a number) sharing = metadata")

        setRootRule(DdlGrammar.ALTER_CLUSTER)
        // ORA-02144 without an option; ORA-02230 for TABLESPACE.
        assertThat(p).notMatches("alter cluster c")
        assertThat(p).notMatches("alter cluster c tablespace users")
    }
}
