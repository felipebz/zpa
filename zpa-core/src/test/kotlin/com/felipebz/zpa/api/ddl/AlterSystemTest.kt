//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterSystemTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_SYSTEM)
    }

    @Test
    fun matchesAlterSystem() {
        assertThat(p).matches("alter system flush shared_pool;")
        assertThat(p).matches("alter system set open_cursors = 500 scope = both;")
    }

    @Test
    fun doesNotMatchAlterSystemWithoutAnAction() {
        assertThat(p).notMatches("alter system;")
    }
}
