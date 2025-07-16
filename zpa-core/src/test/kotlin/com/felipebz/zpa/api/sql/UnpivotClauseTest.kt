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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class UnpivotClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.UNPIVOT_CLAUSE)
    }

    @Test
    fun matchesSimpleUnpivot() {
        assertThat(p).matches("unpivot (amount for quarter in (q1,q2,q3))")
    }

    @Test
    fun matchesSimpleUnpivotWithAliases() {
        assertThat(p).matches("unpivot (amount for quarter in (q1 'q1', q2 as q2, q3 as q3))")
    }

    @Test
    fun matchesSimpleUnpivotIncludeNulls() {
        assertThat(p).matches("unpivot include nulls (amount for quarter in (q1,q2,q3))")
    }

    @Test
    fun matchesSimpleUnpivotExcludeNulls() {
        assertThat(p).matches("unpivot exclude nulls (amount for quarter in (q1,q2,q3))")
    }

    @Test
    fun matchesSimpleUnpivotWithMultipleColumns() {
        assertThat(p).matches(
            """
            unpivot (
              (amount, units)
              for quarter
              in (
                (q1_amount, q1_units) as 'q1',
                (q2_amount, q2_units) as 'q2'
              )
            )
            """
        )
    }
}
