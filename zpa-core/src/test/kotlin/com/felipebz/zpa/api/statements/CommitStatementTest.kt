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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CommitStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.COMMIT_STATEMENT)
    }

    @Test
    fun matchesSimpleCommit() {
        assertThat(p).matches("commit;")
    }

    @Test
    fun matchesCommitWork() {
        assertThat(p).matches("commit work;")
    }

    @Test
    fun matchesCommitForce() {
        assertThat(p).matches("commit force 'test';")
    }

    @Test
    fun matchesCommitForceWithScn() {
        assertThat(p).matches("commit force 'test',1;")
    }

    @Test
    fun matchesCommitWithComment() {
        assertThat(p).matches("commit comment 'test';")
    }

    @Test
    fun matchesCommitWrite() {
        assertThat(p).matches("commit write;")
    }

    @Test
    fun matchesCommitWriteImmediate() {
        assertThat(p).matches("commit write immediate;")
    }

    @Test
    fun matchesCommitWriteBatch() {
        assertThat(p).matches("commit write batch;")
    }

    @Test
    fun matchesCommitWriteWait() {
        assertThat(p).matches("commit write wait;")
    }

    @Test
    fun matchesCommitWriteNoWait() {
        assertThat(p).matches("commit write nowait;")
    }

    @Test
    fun matchesLongCommitStatement() {
        assertThat(p).matches("commit work comment 'teste' write immediate wait;")
    }

    @Test
    fun matchesLabeledCommit() {
        assertThat(p).matches("<<foo>> commit;")
    }

}
