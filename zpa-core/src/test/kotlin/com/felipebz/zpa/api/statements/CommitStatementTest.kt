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

    @Test
    fun matchesWriteOptionsInBothOrders() {
        listOf(
            "commit write immediate wait;", "commit write wait immediate;", "commit write batch nowait;", "commit write nowait batch;",
            "commit write immediate nowait;", "commit write batch wait;", "commit write wait batch;", "commit write nowait immediate;",
            "commit comment 'TEST' write nowait immediate;", "commit work write wait batch;",
            "commit work comment 'x' write batch wait;", "commit work comment 'x' write nowait immediate;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsRepeatedAndMisplacedWriteOptions() {
        listOf(
            "commit write wait nowait;", "commit write nowait wait;", "commit write immediate batch;", "commit write batch immediate;",
            "commit write wait wait;", "commit write immediate immediate;", "commit write wait wait immediate;",
            "commit write wait immediate wait;", "commit write immediate wait nowait;", "commit write wait immediate batch;",
            "commit write bogus;", "commit write wait bogus;", "commit write immediate,;", "commit immediate;", "commit wait;",
            "commit write nowait immediate comment 'x';", "commit comment 'x' comment 'y';", "commit force 'x' write wait;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsWriteOptionTokensUnderTheCommitStatement() {
        val tokens = p.parse("commit comment 'TEST' write nowait immediate;").tokens.map { it.originalValue }
        org.assertj.core.api.Assertions.assertThat(tokens).containsExactly("commit", "comment", "'TEST'", "write", "nowait", "immediate", ";")
        val plain = p.parse("commit work;").tokens.map { it.originalValue }
        org.assertj.core.api.Assertions.assertThat(plain).containsExactly("commit", "work", ";")
    }
}
