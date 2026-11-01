//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.matchers

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.datatype.CharacterDatatype
import com.felipebz.zpa.api.symbols.datatype.DateDatatype
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.asSemantic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MethodMatcherWithTypesTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesMethodWhenTheTypeIsNotSpecifiedInTheMatcher() {
        val matcher = MethodMatcher.create().name("func").addParameter()
        val node = getAstNodeWithArguments("func(x)", NumericDatatype())
        assertThat(matcher.matches(node)).isTrue
    }

    @Test
    fun matchesMethodWhenTheTypeIsSpecifiedAsUnknownInTheMatcher() {
        val matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.UNKNOWN)
        val node = getAstNodeWithArguments("func(x)", NumericDatatype())
        assertThat(matcher.matches(node)).isTrue
    }

    @Test
    fun matchesMethodWhenTheTypeIsSpecifiedInTheMatcher() {
        val matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC)
        val node = getAstNodeWithArguments("func(x)", NumericDatatype())
        assertThat(matcher.matches(node)).isTrue
    }

    @Test
    fun notMatchesMethodWhenTheTypeIsDifferentFromExpectation() {
        val matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.CHARACTER)
        val node = getAstNodeWithArguments("func(x)", NumericDatatype())
        assertThat(matcher.matches(node)).isFalse
    }

    @Test
    fun matchesMethodWithMultipleParametersWhenTheTypeIsSpecifiedInTheMatcher() {
        val matcher = MethodMatcher.create().name("func")
                .addParameters(PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.DATE)
        val node = getAstNodeWithArguments("func(x, y, z)", NumericDatatype(), CharacterDatatype(), DateDatatype())
        assertThat(matcher.matches(node)).isTrue
    }

    @Test
    fun noMatchesMethodWithMultipleParametersWhenTheAnyTypeIsDifferentFromExpectation() {
        val matcher = MethodMatcher.create().name("func")
                .addParameters(PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.DATE)
        val node = getAstNodeWithArguments("func(x, y, z)", NumericDatatype(), CharacterDatatype(), CharacterDatatype())
        assertThat(matcher.matches(node)).isFalse
    }

    private fun getAstNodeWithArguments(text: String, vararg types: PlSqlDatatype): SemanticAstNode {
        val node = p.parse(text).firstChild

        val arguments = node.getDescendants(PlSqlGrammar.ARGUMENT)

        for (i in types.indices) {
            val actualArgument = arguments[i].firstChild
            MethodMatcher.semantic(actualArgument).plSqlDatatype = types[i]
        }

        return node.asSemantic()
    }

}
