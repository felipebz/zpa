//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TrimExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleTrim() {
        assertThat(p).matches("trim(foo)")
    }

    @Test
    fun matchesTrimCharacterFrom() {
        assertThat(p).matches("trim(',' from foo)")
    }

    @Test
    fun matchesTrimLeadingCharacterFrom() {
        assertThat(p).matches("trim(leading ',' from foo)")
    }

    @Test
    fun matchesTrimTrailingCharacterFrom() {
        assertThat(p).matches("trim(trailing ',' from foo)")
    }

    @Test
    fun matchesTrimBothCharacterFrom() {
        assertThat(p).matches("trim(both ',' from foo)")
    }


    @Test
    fun matchesTrimWithoutTrimCharacter() {
        assertThat(p).matches("trim(both from foo)")
        assertThat(p).matches("trim(leading from foo)")
        assertThat(p).matches("trim(trailing from foo)")
    }

    @Test
    fun doesNotMatchTrimWithOnlyADirection() {
        assertThat(p).notMatches("trim(both foo)")
    }
}
