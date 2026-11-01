//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class RecoveryTest : RuleTest() {

    @Test
    fun doNotRecover() {
        setErrorRecoveryEnabled(false)
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).notMatches("unrecognized command")
    }

    @Test
    fun matchesRecovery() {
        setErrorRecoveryEnabled(true)
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("unrecognized command")
    }

}
