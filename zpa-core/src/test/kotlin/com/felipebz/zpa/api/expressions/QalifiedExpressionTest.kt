/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
