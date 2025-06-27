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
        assertThat(p).matches("pivot (sum(amount) for quarter in ('q1', 'q2'))")
    }

    @Test
    fun matchesSimplePivotXml() {
        assertThat(p).matches("pivot xml (sum(amount) for quarter in ('q1', 'q2'))")
    }

    @Test
    fun matchesSimplePivotWithAliases() {
        assertThat(p).matches("pivot (sum(quantity_sold) as qty for region in ('north' north, 'south' south))")
    }

    @Test
    fun matchesSimplePivotWithAs() {
        assertThat(p).matches("pivot (sum(quantity_sold) as qty for region in ('north' as north, 'south' as south))")
    }

    @Test
    fun matchesSimplePivotWithSeveralAggregateExpressions() {
        assertThat(p).matches(
            """
            pivot (
              sum(amount ) as total,
              count(*) as count
              for quarter
              in ('q1' as q1, 'q2' as q2, 'q3' as q3, 'q4' as q4)
            )
            """
        )
    }
}
