//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LockTableStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.LOCK_TABLE_STATEMENT)
    }

    @Test
    fun matchesSimpleLockTable() {
        assertThat(p).matches("lock table tab in exclusive mode;")
    }

    @Test
    fun matchesLockTableWithSchema() {
        assertThat(p).matches("lock table schema.tab in exclusive mode;")
    }

    @Test
    fun matchesLockTableWithMultipleTables() {
        assertThat(p).matches("lock table tab1, tab2 in share mode;")
    }

    @Test
    fun matchesLockTableWithPartition() {
        assertThat(p).matches("lock table tab partition (part1) in exclusive mode;")
    }

    @Test
    fun matchesLockTableModes() {
        assertThat(p).matches("lock table tab in row share mode;")
        assertThat(p).matches("lock table tab in row exclusive mode;")
        assertThat(p).matches("lock table tab in share update mode;")
        assertThat(p).matches("lock table tab in share row exclusive mode;")
    }

    @Test
    fun matchesLockTableWithNowait() {
        assertThat(p).matches("lock table tab in exclusive mode nowait;")
    }

    @Test
    fun matchesLockTableWithWait() {
        assertThat(p).matches("lock table tab in exclusive mode wait 5;")
    }

    @Test
    fun matchesLockTableWithLabel() {
        assertThat(p).matches("<<foo>> lock table tab in exclusive mode;")
    }
}
