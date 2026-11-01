//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

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
