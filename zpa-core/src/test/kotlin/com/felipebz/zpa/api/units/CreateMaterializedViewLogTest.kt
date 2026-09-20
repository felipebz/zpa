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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateMaterializedViewLogTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_MATERIALIZED_VIEW_LOG)
    }

    @Test
    fun matchesPhysicalAttributes() {
        assertThat(p).matches(
            "create materialized view log on customers pctfree 5 tablespace example " +
                "storage (initial 10k);"
        )
        assertThat(p).matches(
            "create materialized view log if not exists on sch.customers sharing = metadata " +
                "logging cache parallel 4;"
        )
    }

    @Test
    fun matchesWithClauseVariants() {
        assertThat(p).matches("create materialized view log on customers with primary key, rowid;")
        assertThat(p).matches(
            "create materialized view log on sales with rowid, sequence(amount_sold, time_id, prod_id) " +
                "including new values;"
        )
        assertThat(p).matches(
            "create materialized view log on product_information " +
                "with rowid, sequence(list_price, min_price, category_id), primary key " +
                "excluding new values;"
        )
        assertThat(p).matches("create materialized view log on order_items with (product_id);")
        assertThat(p).matches("create materialized view log on objects with object id, commit scn;")
    }

    @Test
    fun matchesPurgeAndRefreshClauses() {
        assertThat(p).matches(
            "create materialized view log on orders purge repeat interval '5' day;"
        )
        assertThat(p).matches(
            "create materialized view log on orders purge start with sysdate next sysdate + 1;"
        )
        assertThat(p).matches(
            "create materialized view log on orders purge immediate asynchronous for fast refresh;"
        )
        assertThat(p).matches(
            "create materialized view log on orders for synchronous refresh using mystage_log;"
        )
    }

    @Test
    fun rejectsMaterializedViewSyntax() {
        assertThat(p).notMatches("create materialized view orders as select 1 from dual;")
        assertThat(p).notMatches("create materialized view log on orders with rowid purge;")
        assertThat(p).notMatches("create materialized view log on orders for refresh;")
    }
}
