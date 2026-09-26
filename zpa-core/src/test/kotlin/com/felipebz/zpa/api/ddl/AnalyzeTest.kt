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

class AnalyzeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ANALYZE_STATEMENT)
    }

    @Test
    fun matchesStatisticsAndChainedRows() {
        assertThat(p).matches("analyze table orders delete statistics;")
        assertThat(p).matches("analyze index hr.ix delete system statistics")
        assertThat(p).matches("analyze table orders list chained rows into chained_rows;")
        assertThat(p).matches("analyze cluster c list chained rows")
        assertThat(p).matches("analyze table hr.t partition (p1) delete statistics")
        assertThat(p).matches("analyze table t subpartition (s1) delete statistics")
    }

    @Test
    fun matchesValidationClauses() {
        assertThat(p).matches("analyze index inv_product_ix validate structure;")
        assertThat(p).matches("analyze table employees validate structure cascade;")
        assertThat(p).matches("analyze table t validate structure online")
        assertThat(p).matches("analyze table t validate structure cascade fast")
        assertThat(p).matches("analyze table t validate structure cascade complete offline into hr.invalid_rows")
        assertThat(p).matches("analyze table t validate structure cascade online")
        assertThat(p).matches("analyze table t validate structure into invalid_rows")
        assertThat(p).matches("analyze table customers validate ref update;")
        assertThat(p).matches("analyze table t validate ref update set dangling to null")
        assertThat(p).matches("analyze cluster personnel validate structure cascade;")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-01490 without an action.
        assertThat(p).notMatches("analyze table t")
        // ORA-00906: only the parenthesized partition name forms.
        assertThat(p).notMatches("analyze table t partition for (5) delete statistics")
        assertThat(p).notMatches("analyze table t partition p1 delete statistics")
        // ORA-14052: no partition after a cluster.
        assertThat(p).notMatches("analyze cluster c partition (p1) delete statistics")
        // ORA-03048: nothing follows CASCADE FAST, and ONLINE cannot precede CASCADE.
        assertThat(p).notMatches("analyze table t validate structure cascade fast online")
        assertThat(p).notMatches("analyze table t validate structure cascade fast into t2")
        assertThat(p).notMatches("analyze table t validate structure online cascade")
        assertThat(p).notMatches("analyze table t delete statistics validate structure")
    }
}
