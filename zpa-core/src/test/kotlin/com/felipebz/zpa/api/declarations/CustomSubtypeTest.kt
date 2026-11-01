//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CustomSubtypeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CUSTOM_SUBTYPE)
    }

    @Test
    fun matchesSimpleSubtype() {
        assertThat(p).matches("subtype sub is char;")
    }

    @Test
    fun matchesSubtypeWithScale() {
        assertThat(p).matches("subtype sub is number(5);")
    }

    @Test
    fun matchesSubtypeWithScaleAndPrecision() {
        assertThat(p).matches("subtype sub is number(5,1);")
    }

    @Test
    fun matchesSubtypeWithNotNullConstraint() {
        assertThat(p).matches("subtype sub is number not null;")
    }

    @Test
    fun matchesSubtypeWithRange() {
        assertThat(p).matches("subtype sub is number range 1..2;")
    }


    @Test
    fun matchesSubtypeWithRangeBoundsGivenByConstants() {
        assertThat(p).matches("subtype t_code is pls_integer range gc_min .. gc_max;")
        assertThat(p).matches("subtype t_code is pls_integer range -20999 .. -20000;")
    }

    @Test
    fun doesNotMatchSubtypeWithAnIncompleteRange() {
        assertThat(p).notMatches("subtype t_code is pls_integer range gc_min;")
    }
}
