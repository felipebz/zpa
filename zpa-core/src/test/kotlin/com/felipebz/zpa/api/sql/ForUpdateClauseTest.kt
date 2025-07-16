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
