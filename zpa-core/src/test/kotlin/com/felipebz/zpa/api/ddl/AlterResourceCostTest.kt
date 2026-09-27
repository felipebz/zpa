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

class AlterResourceCostTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_RESOURCE_COST)
    }

    @Test
    fun matchesEachResourceAndFixtureOrder() {
        for (resource in listOf("cpu_per_session", "connect_time", "logical_reads_per_session", "private_sga")) {
            assertThat(p).matches("alter resource cost $resource 1;")
        }
        assertThat(p).matches("alter resource cost cpu_per_session 100 connect_time 1;")
        assertThat(p).matches("alter resource cost logical_reads_per_session 2 connect_time 0;")
        assertThat(p).matches("alter resource cost private_sga 4 logical_reads_per_session 3 connect_time 2 cpu_per_session 1")
        // ORA-02376: redundant resources are checked after the list has parsed.
        assertThat(p).matches("alter resource cost cpu_per_session 1 cpu_per_session 2")
        assertThat(p).matches("alter resource cost connect_time 1 private_sga 2 connect_time 3")
    }

    @Test
    fun distinguishesNumericTokensFromMalformedValues() {
        assertThat(p).matches("alter resource cost connect_time 0")
        assertThat(p).matches("alter resource cost cpu_per_session 1e2")
        assertThat(p).matches("alter resource cost cpu_per_session 1e+2")
        assertThat(p).matches("alter resource cost cpu_per_session 1.0")
        // Oracle checks integrality after parsing a numeric token (ORA-02017).
        assertThat(p).matches("alter resource cost cpu_per_session 1.5")
        assertThat(p).notMatches("alter resource cost cpu_per_session -1")
        assertThat(p).notMatches("alter resource cost cpu_per_session +1")
        assertThat(p).notMatches("alter resource cost cpu_per_session :n")
        assertThat(p).notMatches("alter resource cost cpu_per_session 1+2")
        assertThat(p).notMatches("alter resource cost cpu_per_session (1)")
    }

    @Test
    fun rejectsInvalidResourceListsAndRoutesThroughDdl() {
        for (sql in listOf(
            "alter resource cost",
            "alter resource cost cpu_per_session",
            "alter resource cost unknown_resource 1",
            "alter resource cost cpu_per_session 1 unknown_resource 2",
            "alter resource cost cpu_per_session 1, connect_time 2",
            "alter resource cost cpu_per_session 1,",
            "alter resource cost 123 1"
        )) assertThat(p).notMatches(sql)
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter resource cost cpu_per_session 100 connect_time 1;")
        assertThat(p).matches("alter resource cost logical_reads_per_session 2 connect_time 0;")
    }
}
