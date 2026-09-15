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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

@OptIn(ZpaExperimentalApi::class)
internal class AstVariableDeclaration(private val node: AstNode) : VariableDeclaration {

    override val astNode: AstNode
        get() = node

    override val nameAstNode: AstNode by lazy {
        node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
    }

    override val name: String
        get() = nameAstNode.tokenOriginalValue

    override val isConstant: Boolean
        get() = node.hasDirectChildren(PlSqlKeyword.CONSTANT)

    override val datatypeAstNode: AstNode by lazy {
        node.getFirstChild(PlSqlGrammar.DATATYPE)
    }

    override val nullability: VariableNullability
        get() {
            val constraint = node.getFirstChildOrNull(PlSqlGrammar.DATATYPE_NULL_CONSTRAINT)
                ?: return VariableNullability.UNSPECIFIED
            return if (constraint.hasDirectChildren(PlSqlKeyword.NOT)) {
                VariableNullability.NOT_NULL
            } else {
                VariableNullability.NULLABLE
            }
        }

    private val initializerNode: AstNode?
        get() = node.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)

    override val initializerKind: VariableInitializerKind?
        get() = when {
            initializerNode?.hasDirectChildren(PlSqlPunctuator.ASSIGNMENT) == true -> VariableInitializerKind.ASSIGNMENT
            initializerNode?.hasDirectChildren(PlSqlKeyword.DEFAULT) == true -> VariableInitializerKind.DEFAULT
            else -> null
        }

    override val initializerAstNode: AstNode?
        get() = initializerNode?.lastChildOrNull

}
