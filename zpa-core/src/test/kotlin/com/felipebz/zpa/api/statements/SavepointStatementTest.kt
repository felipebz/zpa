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

class SavepointStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.SAVEPOINT_STATEMENT)
    }

    @Test
    fun matchesSavepoint() {
        assertThat(p).matches("savepoint save;")
    }

    @Test
    fun matchesLabeledSavepoint() {
        assertThat(p).matches("<<foo>> savepoint save;")
    }

}
