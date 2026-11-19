//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword

internal object NullRejectingConditions {

    /** The operands of [condition] a null makes it never true, when it is a condition that rejects null. */
    fun operands(condition: AstNode): List<AstNode> = when (condition.type) {
        PlSqlGrammar.COMPARISON_EXPRESSION -> {
            val operator = condition.getFirstChildOrNull(ConditionsGrammar.RELATIONAL_OPERATOR)
            when {
                // `x <> ALL (…)` is true for a null x when the list is empty.
                operator != null -> if (operator.children.size == 1) listOf(condition.firstChild, condition.lastChild) else emptyList()
                // `x NOT BETWEEN a AND b` is `x < a OR x > b`: a null bound leaves the other comparison to decide.
                condition.hasDirectChildren(PlSqlKeyword.BETWEEN) ->
                    if (condition.hasDirectChildren(PlSqlKeyword.NOT)) listOf(condition.firstChild) else condition.children.filter { it.type !is PlSqlKeyword }
                else -> emptyList()
            }
        }
        ConditionsGrammar.LIKE_CONDITION -> condition.children.filter { it.type !is PlSqlKeyword }.take(2)
        PlSqlGrammar.IN_EXPRESSION -> if (condition.hasDescendant(DmlGrammar.SELECT_EXPRESSION)) emptyList() else listOf(condition.firstChild)
        ConditionsGrammar.BOOLEAN_TEST_CONDITION ->
            if (condition.hasDirectChildren(PlSqlKeyword.NOT) && condition.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) listOf(condition.firstChild)
            else emptyList()
        else -> emptyList()
    }

    /** The top-level conjuncts of [expression]: itself, or the operands of its `AND` chain, through parentheses. */
    fun conjuncts(expression: AstNode): List<AstNode> = when {
        expression.type === PlSqlGrammar.AND_EXPRESSION -> expression.children.filter { it.type !== PlSqlKeyword.AND }.flatMap(::conjuncts)
        expression.type === PlSqlGrammar.BRACKED_EXPRESSION && expression.children.size == 3 -> conjuncts(expression.children[1])
        else -> listOf(expression)
    }
}
