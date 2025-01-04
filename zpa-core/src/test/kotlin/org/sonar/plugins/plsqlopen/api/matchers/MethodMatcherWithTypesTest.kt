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
package org.sonar.plugins.plsqlopen.api.matchers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plsqlopen.asSemantic
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.datatype.CharacterDatatype
import org.sonar.plugins.plsqlopen.api.symbols.datatype.DateDatatype
import org.sonar.plugins.plsqlopen.api.symbols.datatype.NumericDatatype
import org.sonar.plugins.plsqlopen.api.symbols.datatype.PlSqlDatatype

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
