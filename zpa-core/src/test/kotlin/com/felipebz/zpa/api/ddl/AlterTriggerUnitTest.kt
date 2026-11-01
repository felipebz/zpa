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

class AlterTriggerUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_TRIGGER)
    }

    @Test
    fun matchesAlterTriggerDisable() {
        assertThat(p).matches("alter trigger foo disable;")
    }

    @Test
    fun matchesAlterTriggerEnable() {
        assertThat(p).matches("alter trigger foo enable;")
    }

    @Test
    fun matchesAlterTriggerDisableWithSchema() {
        assertThat(p).matches("alter trigger foo.bar disable;")
    }

    @Test
    fun matchesAlterTriggerDisableWithQuotedIdentifier() {
        assertThat(p).matches("alter trigger \"foo\" rename to \"bar\";")
    }

    @Test
    fun matchesAlterTriggerCompile() {
        assertThat(p).matches("alter trigger foo compile;")
    }

    @Test
    fun matchesAlterTriggerIfExists() {
        assertThat(p).matches("alter trigger if exists foo compile;")
    }

}
