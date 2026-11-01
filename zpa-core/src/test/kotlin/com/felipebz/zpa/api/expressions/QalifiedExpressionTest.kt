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

class QalifiedExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.QUALIFIED_EXPRESSION)
    }

    @Test
    fun matchesNamedChoice() {
        assertThat(p).matches("foo(b | b => 1)")
    }

    @Test
    fun matchesIndexedChoice() {
        assertThat(p).matches("foo(1 => 'A', 2 => 'B', 3 | 4 => 'C', fun() => 4)")
    }

    @Test
    fun matchesSequenceIteratorChoice() {
        assertThat(p).matches("foo(for i in 1..10 sequence => i)")
    }

    @Test
    fun matchesBasicIteratorChoice() {
        assertThat(p).matches("foo(for i in 1..10 => i)")
    }

    @Test
    fun matchesIndexIteratorChoice() {
        assertThat(p).matches("foo(for i in 1..10 index i => i)")
    }

    @Test
    fun matchesOthersChoice() {
        assertThat(p).matches("foo(a | b => 3, others => 0)")
    }

}
