/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FloatingPointConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesAllFloatingPointConditions() {
        listOf(
            "value is nan",
            "value is not nan",
            "value is infinite",
            "value is not infinite",
            "1 is not NaN",
            "(1 + 2) IS NOT INFINITE",
            "cast(1 as binary_float) is nan",
            "binary_float_infinity is infinite",
            "cast(1 as binary_double) is not nan",
            "binary_double_nan is nan"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun retainsDedicatedConditionAstBoundary() {
        val node = p.parse("value is not nan")
        assertThatAst(node.getDescendants(ConditionsGrammar.FLOATING_POINT_CONDITION)).hasSize(1)
        assertThatAst(node.getDescendants(ConditionsGrammar.BOOLEAN_TEST_CONDITION)).isEmpty()
    }

    @Test
    fun doesNotEnforceExpressionDatatypesDuringParsing() {
        assertThat(p).matches("'abc' is nan")
        assertThat(p).matches("date '2026-01-01' is infinite")
    }

    @Test
    fun rejectsIncompleteAndOverlongConditions() {
        listOf(
            "value is",
            "value is not",
            "value is nan infinite",
            "value is not not nan",
            "value is nan()",
            "value is not null nan",
            "value is identifier"
        ).forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun preservesOtherIsConditions() {
        assertThat(p).matches("value is null")
        assertThat(p).matches("value is not true")
        assertThat(p).matches("value is json")
    }

    @Test
    fun nanAndInfiniteRemainIdentifiersOutsideConditions() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table fp (nan number, infinite binary_float);")
        assertThat(p).matches("select nan as infinite, infinite as nan from fp;")
        assertThat(p).matches("select nan() as infinite, infinite() as nan from dual;")
    }
}
