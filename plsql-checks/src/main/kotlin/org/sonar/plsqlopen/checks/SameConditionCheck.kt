/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.checks

import com.sonar.sslr.api.AstNode
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SameConditionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.AND_EXPRESSION)
        subscribeTo(PlSqlGrammar.OR_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val expressions = getExpressions(node)
        findSameConditions(expressions)
    }

    private fun findSameConditions(conditions: List<AstNode>) {
        for (i in 1 until conditions.size) {
            checkCondition(conditions, i)
        }
    }

    private fun checkCondition(conditions: List<AstNode>, index: Int) {
        val condition = conditions[index]
        for (j in 0 until index) {
            val otherCondition = conditions[j]
            if (CheckUtils.equalNodes(otherCondition, condition)) {
                addIssue(condition, getLocalizedMessage(), otherCondition.token.line)
                        .secondary(otherCondition, "Original")
                return
            }
        }
    }

    private fun getExpressions(node: AstNode): List<AstNode> {
        val expressions = ArrayList<AstNode>()
        for (subnode in node.children) {
            if (!subnode.typeIs(PlSqlKeyword.AND) && !subnode.typeIs(PlSqlKeyword.OR)) {
                expressions.add(subnode)
            }
        }
        return expressions
    }

}
