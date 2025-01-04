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
package org.sonar.plsqlopen.checks

import com.felipebz.flr.api.AstNode
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class DuplicatedValueInInCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IN_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val values = getInValue(node)
        findSameValues(values)
    }

    private fun getInValue(inExpression: AstNode): List<AstNode> {
        val values = ArrayList<AstNode>()
        var current = inExpression.getFirstChildOrNull(PlSqlPunctuator.LPARENTHESIS)
        while (current != null) {
            current = current.nextSibling

            if (current.typeIs(PlSqlPunctuator.RPARENTHESIS)) {
                current = null
            } else if (!current.typeIs(PlSqlPunctuator.COMMA)) {
                values.add(current)
            }
        }
        return values
    }

    private fun findSameValues(values: List<AstNode>) {
        for (i in 1 until values.size) {
            checkValue(values, i)
        }
    }

    private fun checkValue(values: List<AstNode>, index: Int) {
        val current = values[index]

        for (j in 0 until index) {
            val other = values[j]

            if (CheckUtils.equalNodes(current, other)) {
                addIssue(current, getLocalizedMessage(), current.tokenOriginalValue)
                        .secondary(other, "Original")
                return
            }
        }
    }

}
