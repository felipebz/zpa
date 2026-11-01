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
