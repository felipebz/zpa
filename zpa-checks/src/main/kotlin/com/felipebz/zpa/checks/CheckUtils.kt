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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.matchers.MethodMatcher
import com.felipebz.zpa.typeIs

object CheckUtils {

    private val TERMINATION_STATEMENTS = arrayOf<AstNodeType>(PlSqlGrammar.RETURN_STATEMENT, PlSqlGrammar.EXIT_STATEMENT, PlSqlGrammar.CONTINUE_STATEMENT, PlSqlGrammar.RAISE_STATEMENT)

    private val PROGRAM_UNITS = arrayOf<AstNodeType>(PlSqlGrammar.ANONYMOUS_BLOCK, PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.CREATE_FUNCTION, PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.CREATE_PACKAGE_BODY)

    private val WHEN = arrayOf<AstNodeType>(PlSqlKeyword.WHEN)

    private val NVL_MATCHER = MethodMatcher.create().name("nvl").addParameters(2)

    private val RAISE_APPLICATION_ERROR_MATCHER = MethodMatcher.create().name("raise_application_error").withNoParameterConstraint()

    val terminationStatements: Array<AstNodeType>
        get() = TERMINATION_STATEMENTS.clone()

    fun isNullLiteralOrEmptyString(node: AstNode): Boolean {
        return node.type === PlSqlGrammar.LITERAL && (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node))
    }

    fun isEmptyString(node: AstNode): Boolean {
        val value = node.getFirstChildOrNull(PlSqlGrammar.CHARACTER_LITERAL)?.tokenValue ?: return false
        val openingQuote = value.indexOf('\'')
        val closingQuote = value.lastIndexOf('\'')
        if (openingQuote < 0 || closingQuote <= openingQuote) {
            return false
        }
        val content = value.substring(openingQuote + 1, closingQuote)
        // An alternative-quoted literal holds one opening and one closing delimiter.
        return if (value.substring(0, openingQuote).endsWith('q', ignoreCase = true)) content.length == 2 else content.isEmpty()
    }

    @JvmStatic
    fun equalNodes(node1: AstNode, node2: AstNode): Boolean {
        val first = skipExpressionsWithoutEffect(node1)
        val second = skipExpressionsWithoutEffect(node2)

        if (first.type != second.type || first.numberOfChildren != second.numberOfChildren) {
            return false
        }

        if (first.numberOfChildren == 0) {
            return first.token.value == second.token.value
        }

        val children1: List<AstNode> = first.children
        val children2: List<AstNode> = second.children
        for (i in children1.indices) {
            if (!equalNodes(children1[i], children2[i])) {
                return false
            }
        }
        return true
    }

    fun containsNode(node1: AstNode, node2: AstNode): Boolean {
        val currentNode = skipParenthesis(node1)
        val nodeToCheck = skipParenthesis(node2)

        val type = currentNode.type

        val descendants = ArrayList<AstNode>()
        if (nodeToCheck.type === type) {
            descendants.add(nodeToCheck)
        }
        descendants.addAll(nodeToCheck.getDescendants(type))

        var probableNode: AstNode? = null
        for (descendant in descendants) {
            if (descendant.tokenValue.equals(currentNode.tokenValue, ignoreCase = true)) {
                probableNode = descendant
            }
        }

        return probableNode != null && equalNodes(probableNode, currentNode)
    }

    fun skipExpressionsWithoutEffect(node: AstNode): AstNode {
        var newNode = skipParenthesis(node)
        newNode = skipNvlWithNull(newNode)
        return newNode
    }

    fun skipParenthesis(node: AstNode): AstNode {
        return if (node.type === PlSqlGrammar.BRACKED_EXPRESSION) {
            node.children[1]
        } else node
    }

    fun skipNvlWithNull(node: AstNode): AstNode {
        if (NVL_MATCHER.matches(node)) {
            val arguments = NVL_MATCHER.getArgumentsValues(node)
            if (isNullLiteralOrEmptyString(arguments[1])) {
                return arguments[0]
            }
        }
        return node
    }

    fun isTerminationStatement(node: AstNode): Boolean {
        return (node.typeIs(TERMINATION_STATEMENTS) || RAISE_APPLICATION_ERROR_MATCHER.matches(node)) && !node.hasDirectChildren(*WHEN)
    }

    fun isProgramUnit(node: AstNode?): Boolean {
        return node != null && node.typeIs(PROGRAM_UNITS)
    }
}
