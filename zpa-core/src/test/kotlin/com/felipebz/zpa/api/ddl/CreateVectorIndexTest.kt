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

class CreateVectorIndexTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_VECTOR_INDEX)
    }

    @Test
    fun matchesBothOrganizationsAndQualifiedTargets() {
        assertThat(p).matches("create vector index galaxies_hnsw_idx on galaxies (embedding) organization inmemory neighbor graph distance cosine with target accuracy 95;")
        assertThat(p).matches("create vector index galaxies_ivf_idx on galaxies (embedding) organization neighbor partitions distance cosine with target accuracy 95;")
        assertThat(p).matches("create vector index if not exists app.ix on app.galaxies (embedding) organization inmemory graph")
        assertThat(p).matches("create vector index ix on galaxies (embedding) organization partitions")
        assertThat(p).matches("create vector index ix on galaxies (embedding, other_embedding) organization neighbor partitions")
        assertThat(p).matches("create vector index ix on galaxies (embedding + other_embedding) organization inmemory neighbor graph")
        assertThat(p).matches("create vector index ix on galaxies (embedding) include (id, name) global organization neighbor partitions")
    }

    @Test
    fun matchesMetricsAndAccuracySemanticBoundaries() {
        for (metric in listOf("cosine", "euclidean", "euclidean_squared", "l2_squared", "dot", "manhattan", "hamming", "jaccard")) {
            assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions distance $metric")
        }
        // Oracle resolves unsupported metrics and range violations after parsing (ORA-51915).
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions distance chebyshev")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy 0")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy 101")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy 99.5")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy (95)")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy 90 distance dot")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with distance cosine")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions distance cosine distance dot")
        assertThat(p).notMatches("create vector index ix on t (v) organization neighbor partitions with target accuracy 1 + 2")
        assertThat(p).notMatches("create vector index ix on t (v) distance cosine organization neighbor partitions")
    }

    @Test
    fun matchesCustomDistanceNamesWithoutWideningOrdinaryMetrics() {
        val hnsw = "create vector index ix on t (v) organization inmemory neighbor graph "
        assertThat(p).matches(hnsw + "distance custom euclidean_sq_vector_distance")
        assertThat(p).matches(hnsw + "distance custom pkg.metric")
        assertThat(p).matches(hnsw + "distance custom app.pkg.metric")
        assertThat(p).matches(hnsw + "with distance custom metric")
        assertThat(p).matches(hnsw + "distance custom \"MixedCase\"")
        // Oracle 26 reports ORA-51970 (package validation), not a parse error, for deeper names.
        assertThat(p).matches(hnsw + "distance custom a.b.c.d")
        // ORA-51969: IVF/custom compatibility is checked after parsing.
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions distance custom pkg.metric")
        // A bare CUSTOM reaches Oracle's invalid-function validation (ORA-51974).
        assertThat(p).matches(hnsw + "distance custom")
        assertThat(p).notMatches(hnsw + "distance a.b")
        assertThat(p).notMatches(hnsw + "distance cosine.extra")
        assertThat(p).notMatches(hnsw + "distance custom a.")
        assertThat(p).notMatches(hnsw + "distance custom a..b")
        assertThat(p).notMatches(hnsw + "distance custom 'metric'")
    }

    @Test
    fun matchesStructuredParametersAndOtherVectorOptions() {
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph with target accuracy 90 parameters (type HNSW, neighbors 40, efconstruction 500)")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions with target accuracy 90 parameters (type IVF, neighbor partitions 10)")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph parameters ()")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph parameters (neighbors)")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph parameters (neighbors 2.5)")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph parameters (efconstruction 100, neighbors 40, neighbors 60)")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph parameters (type IVF, neighbor partitions 10)")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF, samples_per_partition 20, min_vectors_per_partition 1, neighbor partition grouping on)")
        assertThat(p).matches("create vector index ix on t (v) organization inmemory neighbor graph quantization scalar compression ratio 4 parameters (type HNSW, m 20, rescore factor 10, algorithm scalar) distribute by rowid range parallel 2 online")
        assertThat(p).matches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF) online local")
        assertThat(p).notMatches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF,)")
        assertThat(p).notMatches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF neighbor partitions 10)")
        assertThat(p).notMatches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF, , neighbors 2)")
        assertThat(p).notMatches("create vector index ix on t (v) organization neighbor partitions parameters (type IVF, bogus 2)")
    }

    @Test
    fun rejectsInvalidHeaderAndRoutesWithoutWideningOtherIndexes() {
        for (sql in listOf(
            "create unique vector index ix on t (v) organization neighbor partitions",
            "create bitmap vector index ix on t (v) organization neighbor partitions",
            "create vector index ix on t () organization neighbor partitions",
            "create vector index ix on t (v,) organization neighbor partitions",
            "create vector index ix on t (v) organization graph",
            "create vector index ix on t (v) organization inmemory neighbor partitions",
            "create vector index ix on t (v) indextype is ctxsys.context organization neighbor partitions"
        )) assertThat(p).notMatches(sql)
        setRootRule(DdlGrammar.CREATE_INDEX)
        assertThat(p).notMatches("create vector index ix on t(v) organization neighbor partitions")
        setRootRule(DdlGrammar.CREATE_SEARCH_INDEX)
        assertThat(p).notMatches("create vector index ix on t(v) organization neighbor partitions")
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create vector index ix on t(v) organization neighbor partitions;")
    }
}
