/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class PivotClauseTest: RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.PIVOT_CLAUSE)
    }

    @Test
    fun matchesSimplePivot() {
        assertThat(p).matches("PIVOT (SUM(amount) FOR quarter IN ('Q1', 'Q2'))")
    }

    @Test
    fun matchesSimplePivotWithAliases() {
        assertThat(p).matches("PIVOT (SUM(quantity_sold) AS qty FOR region IN ('North' north, 'South' south))")
    }

    @Test
    fun matchesSimplePivotWithAs() {
        assertThat(p).matches("PIVOT (SUM(quantity_sold) AS qty FOR region IN ('North' AS north, 'South' AS south))")
    }

    @Test
    fun matchesSimplePivotWithSeveralAggregateExpressions() {
        assertThat(p).matches("PIVOT (\n" +
            "    SUM(amount) AS total,\n" +
            "    COUNT(*) AS count\n" +
            "    FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2, 'Q3' AS Q3, 'Q4' AS Q4)\n" +
            ")")
    }
}
