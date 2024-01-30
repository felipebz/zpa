/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.statements

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

class SetTransactionStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.SET_TRANSACTION_STATEMENT)
    }

    @Test
    fun matchesSetUnnamedReadOnlyTransaction() {
        assertThat(p).matches("set transaction read only;")
    }

    @Test
    fun matchesSetNamedReadOnlyTransaction() {
        assertThat(p).matches("set transaction read only name 'foo';")
    }

    @Test
    fun matchesSetUnnamedRedWriteTransaction() {
        assertThat(p).matches("set transaction read write;")
    }

    @Test
    fun matchesSetNamedRedWriteTransaction() {
        assertThat(p).matches("set transaction read write name 'foo';")
    }

    @Test
    fun matchesSetUnnamedSerializableTransaction() {
        assertThat(p).matches("set transaction isolation level serializable;")
    }

    @Test
    fun matchesSetNamedSerializableTransaction() {
        assertThat(p).matches("set transaction isolation level serializable name 'foo';")
    }

    @Test
    fun matchesSetUnnamedReadCommitedTransaction() {
        assertThat(p).matches("set transaction isolation level read committed;")
    }

    @Test
    fun matchesSetNamedReadCommitedTransaction() {
        assertThat(p).matches("set transaction isolation level read committed name 'foo';")
    }

    @Test
    fun matchesSetUnnamedTransactionWithFixedRollbackSegment() {
        assertThat(p).matches("set transaction use rollback segment foo;")
    }

    @Test
    fun matchesSetNamedTransactionWithFixedRollbackSegment() {
        assertThat(p).matches("set transaction use rollback segment foo name 'bar';")
    }

    @Test
    fun matchesSimpleSetTransactionName() {
        assertThat(p).matches("set transaction name 'foo';")
    }

    @Test
    fun doesNotMatchEmptySetTransaction() {
        assertThat(p).notMatches("set transaction;")
    }

    @Test
    fun matchesLabeledSetTransactionName() {
        assertThat(p).matches("<<foo>> set transaction name 'foo';")
    }


}
