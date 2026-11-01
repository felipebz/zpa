//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HostAndIndicatorVariableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE)
    }

    @Test
    fun matchesSimpleBindingVariable() {
        assertThat(p).matches(":var")
    }

    @Test
    fun matchesSimpleNumericBindingVariable() {
        assertThat(p).matches(":1")
    }

    @Test
    fun matchesShortIndicatorVariable() {
        assertThat(p).matches(":var:ind")
    }

    @Test
    fun matchesLongIndicatorVariable() {
        assertThat(p).matches(":var indicator :ind")
    }

    @Test
    fun matchesQuestionMarkBinding() {
        assertThat(p).matches("?")
    }

}
