//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class LikeExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesLike() {
        assertThat(p).matches("var like 'test'")
    }

    @Test
    fun matchesNotLike() {
        assertThat(p).matches("var not like 'test'")
    }

    @Test
    fun matchesLikeWithEscape() {
        assertThat(p).matches("var like 'test|_foo' escape '|'")
    }

}
