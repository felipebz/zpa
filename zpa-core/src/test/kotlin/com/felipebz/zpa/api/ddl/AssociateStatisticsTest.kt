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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssociateStatisticsTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ASSOCIATE_STATISTICS)
    }

    @Test
    fun matchesAllTargetFamiliesAndTheirLists() {
        assertThat(p).matches("associate statistics with packages emp_mgmt default selectivity 10;")
        assertThat(p).matches("associate statistics with indexes salary_index default cost (100,5,0);")
        for (family in listOf("functions", "packages", "types", "indexes", "indextypes")) {
            assertThat(p).matches("associate statistics with $family obj using stats_type")
            assertThat(p).matches("associate statistics with $family app.obj, other_obj using app.stats_type")
            // A third component resolves (or fails resolution) after parsing in Oracle 26.
            assertThat(p).matches("associate statistics with $family a.b.c default cost (1,2,3)")
            assertThat(p).notMatches("associate statistics with $family a.b.c.d using stats_type")
            assertThat(p).notMatches("associate statistics with $family obj, using stats_type")
            assertThat(p).notMatches("associate statistics with $family ,obj using stats_type")
        }
        assertThat(p).matches("associate statistics with columns tab.col using null")
        assertThat(p).matches("associate statistics with columns app.tab.col, tab.other_col using app.stats_type")
        assertThat(p).notMatches("associate statistics with columns col using null")
        assertThat(p).notMatches("associate statistics with columns a.b.c.d using null")
        assertThat(p).notMatches("associate statistics with columns tab.col, using null")
    }

    @Test
    fun matchesUsingAndDefaultCombinations() {
        val prefix = "associate statistics with packages pkg "
        assertThat(p).matches(prefix + "using null") // ORA-29936: object-family restriction.
        assertThat(p).matches(prefix + "using schema.stats_type")
        assertThat(p).matches(prefix + "default cost (0,0,0)")
        assertThat(p).matches(prefix + "default cost (1e2,2,3)")
        assertThat(p).matches(prefix + "default cost (1.5,2,3)") // ORA-02017: validated by Oracle.
        assertThat(p).matches(prefix + "default selectivity 10.5")
        assertThat(p).matches(prefix + "default selectivity 101") // Out-of-range values are ignored.
        assertThat(p).matches(prefix + "default cost (1,2,3), default selectivity 10")
        assertThat(p).matches(prefix + "default cost (1,2,3) default selectivity 10")
        assertThat(p).matches(prefix + "default selectivity 10, default cost (1,2,3)")
        assertThat(p).matches(prefix + "default selectivity 10 default cost (1,2,3)")
        assertThat(p).matches(prefix + "default selectivity 10, default selectivity 20") // ORA-29928.
        assertThat(p).matches("associate statistics with indexes ix default selectivity 10") // ORA-29822.
        assertThat(p).matches("associate statistics with columns tab.col default cost (1,2,3)") // ORA-29819.
        assertThat(p).notMatches("associate statistics with packages pkg")
        assertThat(p).notMatches(prefix + "using schema.type.extra")
        assertThat(p).notMatches(prefix + "using stats_type default cost (1,2,3)")
        assertThat(p).notMatches(prefix + "default cost (1,2,3) using stats_type")
        assertThat(p).notMatches(prefix + "default cost (1,2,3), default cost (4,5,6)")
        assertThat(p).notMatches(prefix + "default cost (1,2,3),")
        assertThat(p).notMatches(prefix + "default selectivity 10,")
        assertThat(p).notMatches(prefix + "default selectivity 1+2")
        assertThat(p).notMatches(prefix + "default selectivity -1")
        assertThat(p).notMatches(prefix + "default selectivity :n")
        assertThat(p).notMatches(prefix + "default selectivity (10)")
        assertThat(p).notMatches(prefix + "default selectivity")
        assertThat(p).notMatches(prefix + "default cost (-1,2,3)")
        assertThat(p).notMatches(prefix + "default cost (+1,2,3)")
        assertThat(p).notMatches(prefix + "default cost (1+2,2,3)")
        assertThat(p).notMatches(prefix + "default cost (1,2)")
        assertThat(p).notMatches(prefix + "default cost (1,2,3,4)")
        assertThat(p).notMatches(prefix + "default cost (1,2,)")
        assertThat(p).notMatches(prefix + "default cost (:n,2,3)")
    }

    @Test
    fun restrictsStorageToIndextypeUsingBranch() {
        assertThat(p).matches("associate statistics with indextypes it using stats_type with system managed storage tables")
        assertThat(p).matches("associate statistics with indextypes it using stats_type with user managed storage tables")
        assertThat(p).notMatches("associate statistics with packages p using stats_type with system managed storage tables")
        assertThat(p).notMatches("associate statistics with indexes ix using null with user managed storage tables")
        assertThat(p).notMatches("associate statistics with indextypes it default cost (1,2,3) with system managed storage tables")
        assertThat(p).notMatches("associate statistics with indextypes it with system managed storage tables using stats_type")
        assertThat(p).notMatches("associate statistics with indextypes it using stats_type with system managed storage tables with user managed storage tables")
    }

    @Test
    fun routesThroughDdlWithoutChangingTriggerEventGrammar() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("associate statistics with packages emp_mgmt default selectivity 10;")
        assertThat(p).matches("associate statistics with indexes salary_index default cost (100,5,0);")
    }
}
