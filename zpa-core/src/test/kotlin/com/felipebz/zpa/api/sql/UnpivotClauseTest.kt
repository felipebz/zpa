//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        assertThat(p).matches("unpivot (amount for quarter in (q1 'q1', q2 as 'q2', q3 as 'q3'))")
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

    @Test
    fun matchesUnpivotWithMultipleDescriptorColumns() {
        assertThat(p).matches(
            "unpivot ((amount, units) for (year, quarter) in ((q1_amount, q1_units) as (2025, 'q1'), (q2_amount, q2_units) as (2025, 'q2')))"
        )
    }

    @Test
    fun rejectsIdentifierUnpivotDescriptor() {
        assertThat(p).notMatches("unpivot (amount for quarter in (q1 as q1))")
    }

    @Test
    fun rejectsTrailingCommaInUnpivotMeasureColumns() {
        assertThat(p).notMatches("unpivot ((amount,) for quarter in (q1))")
    }

    @Test
    fun rejectsTrailingCommaInUnpivotForColumns() {
        assertThat(p).notMatches("unpivot (amount for (quarter,) in (q1))")
    }

    @Test
    fun rejectsTrailingCommaInUnpivotInValues() {
        assertThat(p).notMatches("unpivot (amount for quarter in (q1,))")
    }

    @Test
    fun rejectsTrailingCommaInUnpivotDescriptorValues() {
        assertThat(p).notMatches("unpivot (amount for quarter in ((q1) as ('q1',)))")
    }
}
