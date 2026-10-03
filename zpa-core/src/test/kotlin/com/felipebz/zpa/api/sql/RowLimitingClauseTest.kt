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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class RowLimitingClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.ROW_LIMITING_CLAUSE)
    }

    @Test
    fun matchesOffsetRow() {
        assertThat(p).matches("offset 1 row")
    }

    @Test
    fun matchesOffsetRows() {
        assertThat(p).matches("offset 1 rows")
    }

    @Test
    fun matchesFetchRowClauseWithoutRowCount() {
        assertThat(p).matches("fetch first row only")
        assertThat(p).matches("fetch first rows only")
        assertThat(p).matches("fetch next row only")
        assertThat(p).matches("fetch next rows with ties")
    }

    @Test
    fun matchesOffsetAndFetchRowClause() {
        assertThat(p).matches("offset 1 row fetch first 1 row only")
    }

    @Test
    fun matchesFetchFirstRowOnly() {
        assertThat(p).matches("fetch first 1 row only")
    }

    @Test
    fun matchesFetchNextRowOnly() {
        assertThat(p).matches("fetch next 1 row only")
    }

    @Test
    fun matchesFetchFirstRowWithTies() {
        assertThat(p).matches("fetch first 1 row with ties")
    }

    @Test
    fun matchesFetchFirstPercentRowOnly() {
        assertThat(p).matches("fetch first 1 percent row only")
    }


    @Test
    fun matchesExactAndApproximateFetch() {
        assertThat(p).matches("fetch exact first 2 rows only")
        assertThat(p).matches("fetch approx first 20 rows only")
        assertThat(p).matches("fetch approximate next 2 rows only")
        assertThat(p).matches("fetch approx first row only")
        assertThat(p).matches("fetch approx first rows only")
        assertThat(p).matches("fetch approx first 2 percent rows only")
        assertThat(p).matches("fetch approx first 2 rows with ties")
        assertThat(p).matches("offset 1 row fetch approx first 2 rows only")
    }

    @Test
    fun rejectsMisplacedApproximateKeywords() {
        assertThat(p).notMatches("fetch approx exact first 2 rows only")
        assertThat(p).notMatches("fetch approx approx first 2 rows only")
        assertThat(p).notMatches("fetch approx 2 rows only")
        assertThat(p).notMatches("fetch first approx 2 rows only")
        assertThat(p).notMatches("approx fetch first 2 rows only")
    }

    @Test
    fun matchesPartitionedRowLimiting() {
        assertThat(p).matches("fetch first 2 partitions by deptno, 3 rows only")
        assertThat(p).matches("fetch first 2 partition by deptno, 3 rows only")
        assertThat(p).matches("fetch first 2 deptno, 3 rows only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 2 partitions by ename, 3 rows only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 2 ename, 1 sal, 3 rows only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 2 partition by ename, 3 rows only")
        assertThat(p).matches("fetch first 2 + 1 partitions by nvl(deptno, 1), :n rows only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 3 row only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 3 percent rows only")
        assertThat(p).matches("fetch first 2 partitions by deptno, 3 rows with ties")
        assertThat(p).matches("offset 1 row fetch first 2 partitions by deptno, 3 rows only")
        assertThat(p).matches("fetch approx first 2 partitions by deptno, 1 row only")
        assertThat(p).matches("fetch exact first 2 partitions by deptno, 3 rows only")
    }

    @Test
    fun rejectsMalformedPartitionedRowLimiting() {
        assertThat(p).notMatches("fetch first 2 by deptno, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions deptno, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partition deptno, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno, ename, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno desc, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno, rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno,")
        assertThat(p).notMatches("fetch first partitions by deptno, 3 rows only")
        assertThat(p).notMatches("fetch first 2 percent partitions by deptno, 3 rows only")
        assertThat(p).notMatches("fetch first 2, 3 rows only")
        assertThat(p).notMatches("fetch first 2 partitions by deptno, 3 rows only offset 1 row")
    }

    @Test
    fun matchesAccuracyClause() {
        assertThat(p).matches("fetch first 3 rows only with target accuracy 90")
        assertThat(p).matches("fetch first 3 rows only target accuracy 90")
        assertThat(p).matches("fetch first 3 rows only with accuracy 90")
        assertThat(p).matches("fetch first 3 rows only accuracy 90")
        assertThat(p).matches("fetch approx first 3 rows only with target accuracy 90 percent")
        assertThat(p).matches("fetch approx first 3 rows only accuracy 9 + 1")
        assertThat(p).matches("fetch approx first 3 rows only accuracy :a")
        assertThat(p).matches("fetch first 3 rows with ties with target accuracy 90")
        assertThat(p).matches("offset 1 row with target accuracy 90")
        assertThat(p).matches("offset 1 row fetch first 3 rows only with target accuracy 90")
        assertThat(p).matches("fetch first 2 partitions by deptno, 3 rows only accuracy 90")
    }

    @Test
    fun matchesAccuracyParameters() {
        assertThat(p).matches("fetch approx first 3 rows only with target accuracy parameters (efsearch 100)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (neighbor partition probes 5)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (rescore factor 3)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (efsearch 100, neighbor partition probes 5)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (neighbor partition probes 5, efsearch 100)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (rescore factor 3, efsearch :e, neighbor partition probes 1 + 1)")
        assertThat(p).matches("fetch approx first 3 rows only accuracy parameters (efsearch 100, efsearch 200)")
    }

    @Test
    fun rejectsMalformedAccuracyClause() {
        assertThat(p).notMatches("with target accuracy 90")
        assertThat(p).notMatches("fetch first 3 rows only with target 90")
        assertThat(p).notMatches("fetch first 3 rows only with 90")
        assertThat(p).notMatches("fetch first 3 rows only with target accuracy")
        assertThat(p).notMatches("fetch first 3 rows only accuracy 90 percent percent")
        assertThat(p).notMatches("fetch first 3 rows only accuracy 90 accuracy 80")
        assertThat(p).notMatches("fetch first 3 rows only accuracy 90 offset 1 row")
        assertThat(p).notMatches("fetch first 3 rows only accuracy 90 parameters (efsearch 100)")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters ()")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (efsearch)")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (efsearch 100,)")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters efsearch 100")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (efsearch 100) percent")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (neighbor probes 5)")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (neighbor partition 5)")
        assertThat(p).notMatches("fetch first 3 rows only accuracy parameters (rescore 3)")
    }

    @Test
    fun buildsRowLimitingNodes() {
        val tree = p.parse(
            "offset 1 row fetch approx first 2 partitions by deptno, 3 ename, 4 rows only with target accuracy 90 percent")
        val fetch = tree.getFirstChild(DmlGrammar.FETCH_ROW_CLAUSE)
        assertThatAst(fetch.getChildren(DmlGrammar.ROW_LIMITING_PARTITION)).hasSize(2)
        assertThatAst(fetch.getChildren(DmlGrammar.ROW_LIMITING_PARTITION)[0].children.map { it.tokenOriginalValue })
            .containsExactly("2", "partitions", "by", "deptno", ",")
        assertThatAst(fetch.getChildren(DmlGrammar.ROW_LIMITING_PARTITION)[1].children.map { it.tokenOriginalValue })
            .containsExactly("3", "ename", ",")
        val accuracy = tree.getFirstChild(DmlGrammar.ROW_LIMITING_ACCURACY_CLAUSE)
        assertThatAst(accuracy.children.map { it.tokenOriginalValue })
            .containsExactly("with", "target", "accuracy", "90", "percent")
    }
}
