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

class AlterFunctionUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_FUNCTION)
    }

    @Test
    fun matchesAlterFunctionCompile() {
        assertThat(p).matches("alter function foo compile;")
    }

    @Test
    fun matchesAlterFunctionIfExists() {
        assertThat(p).matches("alter function if exists foo compile;")
    }

}
