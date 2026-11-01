//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
