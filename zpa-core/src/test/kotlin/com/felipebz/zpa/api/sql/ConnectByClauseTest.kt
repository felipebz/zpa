//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectByClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.CONNECT_BY_CLAUSE)
    }

    @Test
    fun matchesSimpleConnectBy() {
        assertThat(p).matches("connect by foo = bar")
    }

    @Test
    fun matchesConnectByWithPrior() {
        assertThat(p).matches("connect by prior foo = bar")
    }

    @Test
    fun matchesConnectByWithPriorAlternative() {
        assertThat(p).matches("connect by foo = prior bar")
    }

    @Test
    fun matchesConnectByNoCycle() {
        assertThat(p).matches("connect by nocycle foo = bar")
    }

}
