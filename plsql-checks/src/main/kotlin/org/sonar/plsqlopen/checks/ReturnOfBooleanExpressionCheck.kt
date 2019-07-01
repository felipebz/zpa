/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
import org.sonar.plsqlopen.asTree
import org.sonar.plsqlopen.sslr.IfStatement
import org.sonar.plsqlopen.sslr.TreeWithStatements
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = ReturnOfBooleanExpressionCheck.CHECK_KEY, priority = Priority.MINOR, tags = [Tags.CLUMSY])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ReturnOfBooleanExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val ifStatement = node.asTree<IfStatement>()

        val elseClause = ifStatement.elseClause
        if (!hasElsif(ifStatement) && elseClause != null) {
            val firstBoolean = getBooleanValue(ifStatement)
            val secondBoolean = getBooleanValue(elseClause)

            if (firstBoolean != null && secondBoolean != null
                    && firstBoolean.tokenValue != secondBoolean.tokenValue) {
                addIssue(node, getLocalizedMessage(CHECK_KEY))
            }
        }
    }

    private fun hasElsif(ifStatement: IfStatement): Boolean {
        return ifStatement.elsifClauses.isNotEmpty()
    }

    private fun getBooleanValue(node: TreeWithStatements): AstNode? {
        return extractBooleanValueFromReturn(getStatementFrom(node))
    }

    private fun getStatementFrom(node: TreeWithStatements): AstNode? {
        val statements = node.statements
        return if (statements.size == 1) {
            statements[0]
        } else null
    }

    private fun extractBooleanValueFromReturn(node: AstNode?): AstNode? {
        if (node != null) {
            val child = node.firstChild
            if (child.typeIs(PlSqlGrammar.RETURN_STATEMENT)) {
                val expression = child.getFirstChild(PlSqlGrammar.LITERAL)

                return getBooleanLiteral(expression)
            }
        }
        return null
    }

    private fun getBooleanLiteral(expression: AstNode?): AstNode? {
        return expression?.getFirstChild(PlSqlGrammar.BOOLEAN_LITERAL)
    }

    companion object {
        internal const val CHECK_KEY = "ReturnOfBooleanExpression"
    }

}
