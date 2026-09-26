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

class MaterializedZonemapTest : RuleTest() {

    @Test
    fun matchesCreateOnTable() {
        setRootRule(DdlGrammar.CREATE_MATERIALIZED_ZONEMAP)
        assertThat(p).matches("create materialized zonemap sales_zmap on sales(cust_id, prod_id);")
        assertThat(p).matches("create materialized zonemap if not exists sh.z tablespace users scale 10 cache " +
            "refresh fast on load data movement disable pruning on sh.sales (a)")
        assertThat(p).matches("create materialized zonemap z refresh on data movement on t (a)")
        assertThat(p).matches("create materialized zonemap z refresh complete on t (a)")
    }

    @Test
    fun matchesCreateAsQuery() {
        setRootRule(DdlGrammar.CREATE_MATERIALIZED_ZONEMAP)
        assertThat(p).matches("create materialized zonemap sales_zmap as select sys_op_zone_id(s.rowid), " +
            "min(cust_city), max(cust_city) from sales s left outer join customers c on s.cust_id = c.cust_id " +
            "group by sys_op_zone_id(s.rowid);")
        assertThat(p).matches("create materialized zonemap z nocache refresh force on demand enable pruning " +
            "as (select sys_op_zone_id(rowid), min(a) from t group by sys_op_zone_id(rowid))")
        assertThat(p).matches("create materialized zonemap z as with q as (select 1 a from dual) " +
            "select sys_op_zone_id(rowid), min(a) from q group by sys_op_zone_id(rowid)")
    }

    @Test
    fun matchesAlter() {
        setRootRule(DdlGrammar.ALTER_MATERIALIZED_ZONEMAP)
        assertThat(p).matches("alter materialized zonemap sales_zmap pctfree 20 pctused 50 nocache;")
        assertThat(p).matches("alter materialized zonemap sales_zmap refresh fast on commit;")
        assertThat(p).matches("alter materialized zonemap z refresh on load")
        assertThat(p).matches("alter materialized zonemap if exists sh.z disable pruning")
        assertThat(p).matches("alter materialized zonemap z compile")
        assertThat(p).matches("alter materialized zonemap z rebuild")
        assertThat(p).matches("alter materialized zonemap z unusable")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_MATERIALIZED_ZONEMAP)
        // ORA-02000: ON or AS is required, and the clauses keep their order.
        assertThat(p).notMatches("create materialized zonemap z")
        assertThat(p).notMatches("create materialized zonemap z disable pruning refresh fast on t (a)")
        assertThat(p).notMatches("create materialized zonemap z refresh fast scale 10 on t (a)")
        // ORA-00905: REFRESH needs a method or a trigger. ON needs a column list.
        assertThat(p).notMatches("create materialized zonemap z refresh on t (a)")
        assertThat(p).notMatches("create materialized zonemap z on t")
        assertThat(p).notMatches("create materialized zonemap z on t ()")
        // ORA-00922 / ORA-31956: a single query block without ORDER BY.
        assertThat(p).notMatches("create materialized zonemap z as select a from t order by 1")
        assertThat(p).notMatches("create materialized zonemap z as select a from t union all select a from t")

        setRootRule(DdlGrammar.ALTER_MATERIALIZED_ZONEMAP)
        assertThat(p).notMatches("alter materialized zonemap z")
        assertThat(p).notMatches("alter materialized zonemap z refresh")
        assertThat(p).notMatches("alter materialized zonemap z compile rebuild")
        assertThat(p).notMatches("alter materialized zonemap z initrans 2")
        assertThat(p).notMatches("alter materialized zonemap z scale 10")
    }
}
