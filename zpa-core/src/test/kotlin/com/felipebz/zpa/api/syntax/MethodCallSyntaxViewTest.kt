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
package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.matchers.MethodMatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ZpaExperimentalApi::class)
class MethodCallSyntaxViewTest {

    @Test
    fun exposesTargetComponentsArgumentsAndLinks() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.calls.map { Triple(it.qualifier, it.name, it.databaseLink) })
            .containsExactly(
                Triple(emptyList(), "custom_function", null),
                Triple(emptyList(), "custom_function", null),
                Triple(emptyList(), "custom_function", null),
                Triple(listOf("package"), "custom_function", null),
                Triple(listOf("package"), "custom_function", null),
                Triple(listOf("\"schema\"", "\"package\""), "custom_function", null),
                Triple(listOf("obj"), "method", null),
                Triple(emptyList(), "\"custom_function\"", null),
                Triple(listOf("\"schema\"", "\"package\""), "\"custom_function\"", null),
                Triple(emptyList(), "count", null),
                Triple(emptyList(), "count", null),
                Triple(emptyList(), "outer_function", null),
                Triple(emptyList(), "inner_function", null),
                Triple(listOf("pkg"), "proc", "link"),
                Triple(listOf("pkg"), "proc", "link.domain"),
                Triple(listOf("pkg"), "proc", "link.domain.com"),
                Triple(emptyList(), "foo", null),
                Triple(emptyList(), "named_distinct", null),
                Triple(emptyList(), "sum", null),
                Triple(emptyList(), "to_char", null)
            )

        assertThat(visitor.calls[0].argumentLists).containsExactly(emptyList())
        assertThat(visitor.calls[1].argumentLists.single().map { it.expressionAstNode.tokenOriginalValue })
            .containsExactly("1")
        assertThat(visitor.calls[2].argumentLists.single().map { it.expressionAstNode.tokenOriginalValue })
            .containsExactly("a", "b", "c")

        val namedDistinct = visitor.calls.first { it.name == "named_distinct" }.argumentLists.single().single()
        assertThat(namedDistinct.name).isEqualTo("x")
        assertThat(namedDistinct.isDistinct).isTrue
        assertThat(namedDistinct.expressionAstNode.tokenOriginalValue).isEqualTo("value")

        val named = visitor.calls.first { it.name == "method" }.argumentLists.single().single()
        assertThat(named.name).isEqualTo("x")
        assertThat(named.isDistinct).isFalse
        assertThat(named.expressionAstNode.tokenOriginalValue).isEqualTo("1")

        val distinct = visitor.calls.first { it.name == "count" }.argumentLists.single().single()
        assertThat(distinct.name).isNull()
        assertThat(distinct.isDistinct).isTrue
        assertThat(distinct.expressionAstNode.tokenOriginalValue).isEqualTo("emp_id")
    }

    @Test
    fun keepsMultipleArgumentLists() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        val call = visitor.calls.first { it.name == "foo" }
        assertThat(call.argumentLists).hasSize(2)
        assertThat(call.argumentLists.map { it.single().expressionAstNode.tokenOriginalValue })
            .containsExactly("1", "2")
    }

    @Test
    fun specializedFunctionsAreNotPresentedAsGenericMethodCalls() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.calls.map { it.name })
            .doesNotContain("to_date", "listagg")
        assertThat(visitor.calls.map { it.name }).contains("to_char")
    }

    @Test
    fun typedOnlySubscriptionDoesNotInvokeRawCallbacks() {
        val visitor = TypedOnlyVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.calls).isNotEmpty
        assertThat(visitor.rawVisitedTypes).isEmpty()
        assertThat(visitor.rawLeftTypes).isEmpty()
    }

    @Test
    fun tableAndMethodViewsUseTheirOwnTypedChannels() {
        val visitor = BothViewsVisitor()
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/syntax/method-calls-with-table.sql"), null, visitor)

        assertThat(visitor.tables.map { it.name }).containsExactly("employees")
        assertThat(visitor.calls.map { it.name }).containsExactly("custom_function")
    }

    @Test
    fun methodMatcherKeepsItsSemanticsForTypedCalls() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)
        val call = visitor.calls.first {
            it.qualifier == listOf("package") && it.name == "custom_function" &&
                it.argumentLists.single().size == 2
        }
        val matcher = MethodMatcher.create().packageName("package").name("custom_function").addParameters(2)

        assertThat(matcher.matches(call)).isEqualTo(matcher.matches(call.astNode))
        assertThat(matcher.matches(call)).isTrue
    }

    private fun fixture() = File("src/test/resources/syntax/method-calls.sql")

    private class RecordingVisitor : PlSqlVisitor() {
        val calls = mutableListOf<MethodCall>()

        override fun init() {
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
        }
    }

    private class TypedOnlyVisitor : PlSqlVisitor() {
        val calls = mutableListOf<MethodCall>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }

    private class BothViewsVisitor : PlSqlVisitor() {
        val tables = mutableListOf<TableReference>()
        val calls = mutableListOf<MethodCall>()

        override fun init() {
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { tables += it }
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
        }
    }
}
