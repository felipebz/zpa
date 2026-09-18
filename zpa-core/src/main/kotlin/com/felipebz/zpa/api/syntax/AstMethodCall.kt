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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

@OptIn(ZpaExperimentalApi::class)
internal class AstMethodCall(private val node: AstNode) : MethodCall {

    private companion object {
        private val MEMBER_COMPONENT_TYPES: Set<AstNodeType> = setOf(
            PlSqlGrammar.IDENTIFIER_NAME,
            PlSqlKeyword.COUNT,
            PlSqlKeyword.ROWCOUNT,
            PlSqlKeyword.BULK_ROWCOUNT,
            PlSqlKeyword.FIRST,
            PlSqlKeyword.LAST,
            PlSqlKeyword.LIMIT,
            PlSqlKeyword.NEXT,
            PlSqlKeyword.PRIOR,
            PlSqlKeyword.EXISTS,
            PlSqlKeyword.FOUND,
            PlSqlKeyword.NOTFOUND,
            PlSqlKeyword.ISOPEN,
            PlSqlKeyword.DELETE,
            PlSqlKeyword.TRIM,
            PlSqlKeyword.EXTEND,
            PlSqlKeyword.NEXTVAL,
            PlSqlKeyword.CURRVAL,
        )
    }

    private val targetNode: AstNode by lazy {
        node.children.first { it.type !== PlSqlGrammar.ARGUMENTS }
    }

    private val targetComponents: List<String> by lazy {
        if (targetNode.type === PlSqlGrammar.MEMBER_EXPRESSION) {
            targetNode.children
                .takeWhile { it.type !== PlSqlPunctuator.REMOTE }
                .mapNotNull { componentValue(it) }
        } else {
            listOfNotNull(componentValue(targetNode))
        }
    }

    private val linkComponents: List<String> by lazy {
        if (targetNode.type !== PlSqlGrammar.MEMBER_EXPRESSION) {
            emptyList()
        } else {
            val remoteIndex = targetNode.children.indexOfFirst { it.type === PlSqlPunctuator.REMOTE }
            if (remoteIndex < 0) {
                emptyList()
            } else {
                targetNode.children.drop(remoteIndex + 1).mapNotNull { componentValue(it) }
            }
        }
    }

    override val astNode: AstNode
        get() = node

    override val qualifier: List<String>
        get() = targetComponents.dropLast(1)

    override val name: String
        get() = targetComponents.lastOrNull().orEmpty()

    override val databaseLink: String?
        get() = linkComponents.takeIf { it.isNotEmpty() }?.joinToString(".")

    override val argumentLists: List<List<MethodCallArgument>> by lazy {
        node.getChildren(PlSqlGrammar.ARGUMENTS).map { arguments ->
            arguments.getChildren(PlSqlGrammar.ARGUMENT).map(::AstMethodCallArgument)
        }
    }

    private fun componentValue(node: AstNode): String? = when (node.type) {
        PlSqlGrammar.VARIABLE_NAME -> node.getFirstDescendantOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue
        in MEMBER_COMPONENT_TYPES -> node.tokenOriginalValue
        else -> null
    }
}

@OptIn(ZpaExperimentalApi::class)
private class AstMethodCallArgument(private val node: AstNode) : MethodCallArgument {

    override val astNode: AstNode
        get() = node

    override val name: String?
        get() = node.children
            .takeIf { it.size >= 2 && it[0].type === PlSqlGrammar.IDENTIFIER_NAME && it[1].type === PlSqlPunctuator.ASSOCIATION }
            ?.first()
            ?.tokenOriginalValue

    override val isDistinct: Boolean
        get() = node.children.any { it.type === PlSqlKeyword.DISTINCT }

    override val expressionAstNode: AstNode
        get() = node.lastChild
}
