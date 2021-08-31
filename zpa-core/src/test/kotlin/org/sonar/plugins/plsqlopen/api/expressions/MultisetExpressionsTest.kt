/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api.expressions

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class MultisetExpressionsTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesIsASet() {
        assertThat(p).matches("foo is a set")
    }

    @Test
    fun matchesIsNotASet() {
        assertThat(p).matches("foo is not a set")
    }

    @Test
    fun matchesIsEmpty() {
        assertThat(p).matches("foo is not a set")
    }

    @Test
    fun matchesIsNotEmpty() {
        assertThat(p).matches("foo is not a set")
    }

    @Test
    fun matchesMemberExpression() {
        assertThat(p).matches("foo member bar")
    }

    @Test
    fun matchesMemberOfExpression() {
        assertThat(p).matches("foo member of bar")
    }

    @Test
    fun matchesNotMemberOfExpression() {
        assertThat(p).matches("foo not member of bar")
    }

    @Test
    fun matchesSimpleSubmultiset() {
        assertThat(p).matches("foo submultiset bar")
    }

    @Test
    fun matchesSubmultisetOf() {
        assertThat(p).matches("foo submultiset of bar")
    }

    @Test
    fun matchesNotSubmultiset() {
        assertThat(p).matches("foo not submultiset bar")
    }

    @Test
    fun matchesNotSubmultisetOf() {
        assertThat(p).matches("foo not submultiset of bar")
    }

    @Test
    fun matchesMultisetExcept() {
        assertThat(p).matches("foo multiset except bar")
    }

    @Test
    fun matchesMultisetExceptAll() {
        assertThat(p).matches("foo multiset except all bar")
    }

    @Test
    fun matchesMultisetExceptDistinct() {
        assertThat(p).matches("foo multiset except distinct bar")
    }

    @Test
    fun doesNotMatcheMultisetExceptAllDistinct() {
        assertThat(p).notMatches("foo multiset except all distinct")
    }

    @Test
    fun matchesMultisetIntersect() {
        assertThat(p).matches("foo multiset intersect bar")
    }

    @Test
    fun matchesMultisetIntersectWithFunctions() {
        assertThat(p).matches("foo(1,2,3) multiset intersect bar('a', 'b', 'c')")
    }

    @Test
    fun matchesMultisetUnion() {
        assertThat(p).matches("foo multiset union bar")
    }

    @Test
    fun matchesMultipleMultisetUnion() {
        assertThat(p).matches("a multiset union b multiset union c multiset union d")
    }
}
