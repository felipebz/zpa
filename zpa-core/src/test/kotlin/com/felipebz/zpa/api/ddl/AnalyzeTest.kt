//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    fun matchesComputeAndEstimateStatistics() {
        assertThat(p).matches("analyze table tab compute statistics;")
        assertThat(p).matches("analyze table tab compute statistics for table;")
        assertThat(p).matches("analyze table tab estimate statistics sample 10 percent;")
        assertThat(p).matches("analyze index ix compute statistics;")
        assertThat(p).matches("analyze cluster cl compute statistics;")
        assertThat(p).matches("analyze table tab compute system statistics")
        assertThat(p).matches("analyze table tab estimate system statistics")
        assertThat(p).matches("analyze table tab estimate statistics sample 100 rows")
        assertThat(p).matches("analyze table tab estimate statistics for table sample 10 percent")
        assertThat(p).matches("analyze table tab estimate statistics sample 10 percent for table")
        assertThat(p).matches("analyze table tab estimate statistics for all columns for table sample 5 rows")
        assertThat(p).matches("analyze table tab compute statistics for all indexed columns size 10 for all indexes")
        assertThat(p).matches("analyze table tab compute statistics for columns id, val size 5")
        assertThat(p).matches("analyze table tab compute statistics for columns size 4 id, val")
        assertThat(p).matches("analyze table tab compute statistics for columns id size 3, val size 5")
        assertThat(p).matches("analyze table tab compute statistics for table for all indexes for all columns")
        assertThat(p).matches("analyze table tab partition (p1) compute statistics for all local indexes")
        assertThat(p).matches("analyze index ix estimate statistics sample 5 percent")
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
        // ORA-01490: SAMPLE goes with ESTIMATE, once; ORA-02000: SAMPLE needs ROWS or PERCENT; ORA-02017: an integer.
        assertThat(p).notMatches("analyze table tab compute statistics sample 10 percent")
        assertThat(p).notMatches("analyze table tab estimate statistics sample 10 percent sample 5 rows")
        assertThat(p).notMatches("analyze table tab estimate statistics sample 10")
        assertThat(p).notMatches("analyze table tab estimate statistics sample 10.5 percent")
        // ORA-03049: a comma between columns; ORA-00921: a column after COLUMNS; ORA-02000: COLUMNS after ALL.
        assertThat(p).notMatches("analyze table tab compute statistics for columns id size 3 val size 5")
        assertThat(p).notMatches("analyze table tab compute statistics for columns")
        assertThat(p).notMatches("analyze table tab compute statistics for all")
    }
}
