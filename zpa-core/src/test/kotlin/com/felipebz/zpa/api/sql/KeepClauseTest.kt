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

class KeepClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.KEEP_CLAUSE)
    }

    @Test
    fun matchesKeepFirst() {
        assertThat(p).matches("keep (dense_rank first order by foo)")
    }

    @Test
    fun matchesKeepLast() {
        assertThat(p).matches("keep (dense_rank last order by foo)")
    }

}
