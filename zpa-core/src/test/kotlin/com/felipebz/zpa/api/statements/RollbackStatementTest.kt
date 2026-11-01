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

class RollbackStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ROLLBACK_STATEMENT)
    }

    @Test
    fun matchesSimpleRollback() {
        assertThat(p).matches("rollback;")
    }

    @Test
    fun matchesRollbackWork() {
        assertThat(p).matches("rollback work;")
    }

    @Test
    fun matchesRollbackForce() {
        assertThat(p).matches("rollback force 'test';")
    }

    @Test
    fun matchesRollbackToSavepoint() {
        assertThat(p).matches("rollback to save;")
    }

    @Test
    fun matchesRollbackToSavepointAlternative() {
        assertThat(p).matches("rollback to savepoint save;")
    }

    @Test
    fun matchesLongRollbackStatement() {
        assertThat(p).matches("rollback work to savepoint save;")
    }

    @Test
    fun matchesLabeledRollback() {
        assertThat(p).matches("<<foo>> rollback;")
    }

}
