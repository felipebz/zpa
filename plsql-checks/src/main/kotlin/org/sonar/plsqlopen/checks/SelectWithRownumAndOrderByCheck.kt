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
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("20min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SelectWithRownumAndOrderByCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        if (!hasOrderByClause(node)) {
            return
        }

        val queryBlock = node.getFirstChild(DmlGrammar.QUERY_BLOCK)

        val whereClause = queryBlock.getChildren(DmlGrammar.WHERE_CLAUSE)
        if (whereClause.isEmpty()) {
            return
        }

        val whereComparisonConditions = whereClause[0].getDescendants(PlSqlGrammar.COMPARISON_EXPRESSION)
        if (whereComparisonConditions.isEmpty()) {
            return
        }

        for (comparison in whereComparisonConditions) {
            for (child in comparison.getChildren(PlSqlGrammar.VARIABLE_NAME)) {
                if ("rownum".equals(child.tokenValue, ignoreCase = true) && node == child.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION)) {
                    addIssue(comparison, getLocalizedMessage())
                }
            }
        }
    }

    private fun hasOrderByClause(node: AstNode): Boolean {
        return node.hasDirectChildren(DmlGrammar.ORDER_BY_CLAUSE)
    }

}
