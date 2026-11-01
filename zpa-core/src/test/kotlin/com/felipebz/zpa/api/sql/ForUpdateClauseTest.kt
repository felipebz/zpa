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

class ForUpdateClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.FOR_UPDATE_CLAUSE)
    }

    @Test
    fun matchesSimpleForUpdate() {
        assertThat(p).matches("for update")
    }

    @Test
    fun matchesForUpdateOfColumn() {
        assertThat(p).matches("for update of col")
    }

    @Test
    fun matchesForUpdateOfColumnWithTable() {
        assertThat(p).matches("for update of tab.col")
    }

    @Test
    fun matchesForUpdateOfColumnWithSchemaAndTable() {
        assertThat(p).matches("for update of sch.tab.col")
    }

    @Test
    fun matchesForUpdateOfMultipleColumns() {
        assertThat(p).matches("for update of col, col2, col3")
    }

    @Test
    fun matchesForUpdateNoWait() {
        assertThat(p).matches("for update nowait")
    }

    @Test
    fun matchesForUpdateWait() {
        assertThat(p).matches("for update wait 1")
    }

    @Test
    fun matchesForUpdateSkipLocked() {
        assertThat(p).matches("for update skip locked")
    }

    @Test
    fun matchesLongForUpdate() {
        assertThat(p).matches("for update of col skip locked")
    }

}
