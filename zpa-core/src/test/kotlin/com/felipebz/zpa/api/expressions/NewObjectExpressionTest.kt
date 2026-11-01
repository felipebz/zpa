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

class NewObjectExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleNewObject() {
        assertThat(p).matches("new foo.bar")
    }

    @Test
    fun matchesNewObjectWithEmptyParameterList() {
        assertThat(p).matches("new foo()")
    }

    @Test
    fun matchesNewObjectWithSimpleParameterList() {
        assertThat(p).matches("new foo(1, foo, 'bar', foo + bar)")
    }

    @Test
    fun matchesNewObjectWithNamedParameterList() {
        assertThat(p).matches("new foo(p1 => 1, p2 => foo, p3 => 'bar', p4 => foo + bar)")
    }
}
