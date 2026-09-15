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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ZpaExperimentalApi::class)
class VariableDeclarationSyntaxViewTest {

    @Test
    fun exposesDeclarationProperties() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.declarations.map { it.name }).containsExactly(
            "plain",
            "numeric_precision",
            "constant_number",
            "default_text",
            "not_null_number",
            "nullable_number",
            "custom_type_variable",
            "package_type",
            "column_type",
            "row_type",
            "ref_type",
            "\"MyVariable\"",
        )

        val plain = visitor.declaration("plain")
        assertThat(plain.nameAstNode.tokenOriginalValue).isEqualTo("plain")
        assertThat(plain.isConstant).isFalse
        assertThat(plain.datatypeAstNode.type).isEqualTo(PlSqlGrammar.DATATYPE)
        assertThat(plain.nullability).isEqualTo(VariableNullability.UNSPECIFIED)
        assertThat(plain.initializerKind).isNull()
        assertThat(plain.initializerAstNode).isNull()

        val precision = visitor.declaration("numeric_precision")
        assertThat(precision.datatypeAstNode.type).isEqualTo(PlSqlGrammar.DATATYPE)
        assertThat(precision.nullability).isEqualTo(VariableNullability.UNSPECIFIED)

        val constant = visitor.declaration("constant_number")
        assertThat(constant.isConstant).isTrue
        assertThat(constant.initializerKind).isEqualTo(VariableInitializerKind.ASSIGNMENT)
        assertThat(constant.initializerAstNode?.tokenOriginalValue).isEqualTo("1")

        val defaultValue = visitor.declaration("default_text")
        assertThat(defaultValue.initializerKind).isEqualTo(VariableInitializerKind.DEFAULT)
        assertThat(defaultValue.initializerAstNode?.tokenOriginalValue).isEqualTo("'x'")

        val notNull = visitor.declaration("not_null_number")
        assertThat(notNull.nullability).isEqualTo(VariableNullability.NOT_NULL)
        assertThat(notNull.initializerKind).isEqualTo(VariableInitializerKind.ASSIGNMENT)

        val nullable = visitor.declaration("nullable_number")
        assertThat(nullable.nullability).isEqualTo(VariableNullability.NULLABLE)
        assertThat(nullable.initializerKind).isEqualTo(VariableInitializerKind.DEFAULT)

        listOf("custom_type_variable", "package_type", "column_type", "row_type", "ref_type")
            .map(visitor::declaration)
            .forEach {
                assertThat(it.datatypeAstNode.type).isEqualTo(PlSqlGrammar.DATATYPE)
            }

        val quoted = visitor.declaration("\"MyVariable\"")
        assertThat(quoted.nameAstNode.tokenOriginalValue).isEqualTo("\"MyVariable\"")
    }

    @Test
    fun exceptionDeclarationsAreNotVariableDeclarationViews() {
        val visitor = ExceptionBoundaryVisitor()
        TestPlSqlVisitorRunner.scanFile(exceptionFixture(), null, visitor)

        assertThat(visitor.variables.map { it.name }).containsExactly("local_variable")
        assertThat(visitor.exceptionDeclarations).containsExactly("local_exception")
    }

    @Test
    fun typedOnlySubscriptionDoesNotInvokeRawCallbacks() {
        val visitor = TypedOnlyVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.declarations).hasSize(12)
        assertThat(visitor.rawVisitedTypes).isEmpty()
        assertThat(visitor.rawLeftTypes).isEmpty()
    }

    @Test
    fun multipleTypedViewsReceiveOnlyTheirOwnNodes() {
        val visitor = AllViewsVisitor()
        TestPlSqlVisitorRunner.scanFile(dispatchFixture(), null, visitor)

        assertThat(visitor.variables.map { it.name }).containsExactly("local_value")
        assertThat(visitor.calls.map { it.name }).containsExactly("function_call")
        assertThat(visitor.tables.map { it.name }).containsExactly("table_name")
    }

    private fun fixture() = File("src/test/resources/syntax/variable-declarations.sql")

    private fun dispatchFixture() = File("src/test/resources/syntax/variable-declaration-dispatch.sql")

    private fun exceptionFixture() = File("src/test/resources/syntax/variable-declarations-with-exception.sql")

    private class RecordingVisitor : PlSqlVisitor() {
        val declarations = mutableListOf<VariableDeclaration>()

        override fun init() {
            subscribeTo(SyntaxViews.VARIABLE_DECLARATION) { declarations += it }
        }

        fun declaration(name: String) = declarations.first { it.name == name }
    }

    private class TypedOnlyVisitor : PlSqlVisitor() {
        val declarations = mutableListOf<VariableDeclaration>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(SyntaxViews.VARIABLE_DECLARATION) { declarations += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }

    private class AllViewsVisitor : PlSqlVisitor() {
        val tables = mutableListOf<TableReference>()
        val calls = mutableListOf<MethodCall>()
        val variables = mutableListOf<VariableDeclaration>()

        override fun init() {
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { tables += it }
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
            subscribeTo(SyntaxViews.VARIABLE_DECLARATION) { variables += it }
        }
    }

    private class ExceptionBoundaryVisitor : PlSqlVisitor() {
        val variables = mutableListOf<VariableDeclaration>()
        val exceptionDeclarations = mutableListOf<String>()

        override fun init() {
            subscribeTo(SyntaxViews.VARIABLE_DECLARATION) { variables += it }
            subscribeTo(PlSqlGrammar.EXCEPTION_DECLARATION)
        }

        override fun visitNode(node: AstNode) {
            exceptionDeclarations += node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue
        }
    }
}
