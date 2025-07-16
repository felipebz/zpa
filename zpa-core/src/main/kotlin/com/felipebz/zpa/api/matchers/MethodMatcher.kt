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
package com.felipebz.zpa.api.matchers

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType

class MethodMatcher private constructor()
{

    private var methodNameCriteria: NameCriteria? = null
    private var packageNameCriteria: NameCriteria? = null
    private var schemaNameCriteria: NameCriteria? = null
    private var shouldCheckParameters = true
    private var schemaIsOptional = false
    var methodName: String = ""
        private set
    private val expectedArgumentTypes = ArrayList<PlSqlType>()

    fun name(methodNameCriteria: String) =
        name(NameCriteria.`is`(methodNameCriteria))

    fun name(methodNameCriteria: NameCriteria) = apply {
        check(this.methodNameCriteria == null)
        this.methodNameCriteria = methodNameCriteria
    }

    fun packageName(packageNameCriteria: String) =
        packageName(NameCriteria.`is`(packageNameCriteria))

    fun packageName(packageNameCriteria: NameCriteria) = apply {
        check(this.packageNameCriteria == null)
        this.packageNameCriteria = packageNameCriteria
    }

    fun schema(schemaNameCriteria: String) =
        schema(NameCriteria.`is`(schemaNameCriteria))

    fun schema(schemaNameCriteria: NameCriteria) = apply {
        check(this.schemaNameCriteria == null)
        this.schemaNameCriteria = schemaNameCriteria
    }

    fun withNoParameterConstraint() = apply {
        check(this.expectedArgumentTypes.isEmpty())
        this.shouldCheckParameters = false
    }

    fun schemaIsOptional() = apply {
        this.schemaIsOptional = true
    }

    fun addParameter() =
        addParameter(PlSqlType.UNKNOWN)

    fun addParameter(type: PlSqlType) = apply {
        check(this.shouldCheckParameters)
        expectedArgumentTypes.add(type)
    }

    fun addParameters(quantity: Int) = apply {
        for (i in 0 until quantity) {
            addParameter(PlSqlType.UNKNOWN)
        }
    }

    fun addParameters(vararg types: PlSqlType) = apply {
        check(this.shouldCheckParameters)
        for (type in types) {
            addParameter(type)
        }
    }

    fun getArguments(node: AstNode): List<AstNode> {
        val arguments = node.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)
        return arguments?.getChildren(PlSqlGrammar.ARGUMENT) ?: ArrayList()

    }

    fun getArgumentsValues(node: AstNode) =
        getArguments(node).map { it.lastChild }.toList()

    fun matches(originalNode: AstNode): Boolean {
        val node = normalize(originalNode)

        var i = -1
        val nodes = arrayOfNulls<String>(3)
        for (child in node.children) {
            if (i < 2 && (child.type === PlSqlGrammar.VARIABLE_NAME || child.type === PlSqlGrammar.IDENTIFIER_NAME)) {
                nodes[++i] = child.tokenValue
            }
        }

        fun hasMoreItensToCheck() = i > -1
        fun nextNode() = nodes[i--]

        if (!hasMoreItensToCheck()) {
            return false
        }

        var matches =  methodNameCriteria?.let { nameAcceptable(nextNode(), it) } ?: true

        packageNameCriteria?.let {
            matches = matches and (hasMoreItensToCheck() && nameAcceptable(nextNode(), it))
        }

        schemaNameCriteria?.let {
            matches = matches and (schemaIsOptional && !hasMoreItensToCheck() ||
                hasMoreItensToCheck() && nameAcceptable(nextNode(), it))
        }

        return matches && !hasMoreItensToCheck() && argumentsAcceptable(originalNode)
    }

    private fun nameAcceptable(name: String?, criteria: NameCriteria): Boolean {
        methodName = name ?: ""
        return criteria.matches(methodName)
    }

    private fun argumentsAcceptable(node: AstNode): Boolean {
        val arguments = getArguments(node)
        return !shouldCheckParameters || arguments.size == expectedArgumentTypes.size && argumentTypesAreCorrect(arguments)
    }

    private fun argumentTypesAreCorrect(arguments: List<AstNode>): Boolean {
        var result = true
        for ((i, type) in expectedArgumentTypes.withIndex()) {
            val actualArgument = arguments[i].firstChild
            result = result and (type === PlSqlType.UNKNOWN || type === semantic(actualArgument).plSqlType)
        }
        return result
    }

    private fun normalize(node: AstNode): AstNode {
        if (node.type === PlSqlGrammar.METHOD_CALL || node.type === PlSqlGrammar.CALL_STATEMENT) {
            var child = normalize(node.firstChild)
            if (child.firstChild.type === PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE) {
                child = child.firstChild
            }
            return child
        }
        return node
    }

    companion object {
        @JvmStatic
        fun create(): MethodMatcher {
            return MethodMatcher()
        }

        fun semantic(node: AstNode): SemanticAstNode {
            return node as SemanticAstNode
        }
    }

}
