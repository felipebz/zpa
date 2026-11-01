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

class AlterProcedureUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PROCEDURE)
    }

    @Test
    fun matchesAlterProcedureCompile() {
        assertThat(p).matches("alter procedure foo compile;")
    }

    @Test
    fun matchesAlterProcedureIfExists() {
        assertThat(p).matches("alter procedure if exists foo compile;")
    }

}
