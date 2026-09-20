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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

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
