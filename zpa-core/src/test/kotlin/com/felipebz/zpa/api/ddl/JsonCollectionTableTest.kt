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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class JsonCollectionTableTest : RuleTest() {

    private val salary = "salary as (json_value(data, '$.salary.number()'))"

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    @Test
    fun matchesPlainAndEtagCollections() {
        assertThat(p).matches("create json collection table j")
        assertThat(p).matches("create json collection table j;")
        assertThat(p).matches("create json collection table hr.j with etag")
        assertThat(p).matches("create json collection table j with etag ($salary)")
    }

    @Test
    fun matchesExpressionColumnsAndConstraintsInAnyOrder() {
        assertThat(p).matches("create json collection table j ($salary)")
        assertThat(p).matches("create json collection table j ($salary, constraint c check (salary > 0))")
        assertThat(p).matches("create json collection table j (constraint c check (data is json))")
        assertThat(p).matches("create json collection table j (constraint c check (data is json), $salary)")
        assertThat(p).matches("create json collection table j (a as (json_value(data, '$.a.number()')), b as (json_value(data, '$.b.number()')))")
        assertThat(p).matches("create json collection table j (s generated always as (json_value(data, '$.s.number()')) virtual)")
        assertThat(p).matches("create json collection table j (s number as (json_value(data, '$.s.number()')) stored not null)")
    }

    @Test
    fun matchesPartitioningAndTableProperties() {
        assertThat(p).matches(
            "create json collection table orders (po_num_vc number generated always as " +
                "(json_value (data, '$.PONumber.number()' error on error)) materialized) " +
                "partition by range (po_num_vc) (partition p1 values less than (1000), partition p2 values less than (2000))")
        assertThat(p).matches("create json collection table j with etag ($salary) partition by hash (salary) partitions 2")
        assertThat(p).matches("create json collection table j tablespace users")
        assertThat(p).matches("create json collection table j ($salary) tablespace users enable row movement")
        assertThat(p).matches("create json collection table j as select 1 from dual")
    }

    @Test
    fun rejectsMalformedCollections() {
        assertThat(p).notMatches("create json collection table")
        assertThat(p).notMatches("create json collection table j ()")
        assertThat(p).notMatches("create json collection table j (x number)")
        assertThat(p).notMatches("create json collection table j (s)")
        assertThat(p).notMatches("create json collection table j (s as json_value(data, '$.s.number()'))")
        assertThat(p).notMatches("create json collection table j ($salary,)")
        assertThat(p).notMatches("create json collection table j ($salary $salary)")
        assertThat(p).notMatches("create json collection table j ($salary) with etag")
        assertThat(p).notMatches("create json collection table j with etag with etag")
        assertThat(p).notMatches("create json collection table j with")
        assertThat(p).notMatches("create json collection table j ($salary) ($salary)")
        assertThat(p).notMatches("create json collection table j unexpected")
        assertThat(p).notMatches("create json collection j")
        assertThat(p).notMatches("create json table j")
        assertThat(p).notMatches("create collection table j")
        assertThat(p).notMatches("create global temporary json collection table j")
        assertThat(p).notMatches("create private temporary json collection table j")
        assertThat(p).notMatches("create immutable json collection table j")
    }

    @Test
    fun separatesCollectionColumnsFromOrdinaryTables() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create json collection table j with etag ($salary, constraint c check (salary > 0)) tablespace users; " +
            "create table t (a number, b as (a + 1)); " +
            "create table json (collection number, etag number);")
        val columns = tree.getDescendants(DdlGrammar.JSON_COLLECTION_COLUMNS).single()
        assertThatAst(columns.getChildren(DdlGrammar.VIRTUAL_COLUMN_DEFINITION)).hasSize(1)
        assertThatAst(columns.getChildren(DdlGrammar.OUT_OF_LINE_CONSTRAINT)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_TABLE)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.VIRTUAL_COLUMN_DEFINITION)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.TABLE_COLUMN_DEFINITION)).hasSize(3)
    }
}
