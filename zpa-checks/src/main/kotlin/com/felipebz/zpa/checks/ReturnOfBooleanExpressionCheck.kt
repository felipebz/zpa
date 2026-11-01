//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.syntax.IfStatement
import com.felipebz.zpa.api.syntax.SyntaxViews
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MINOR, tags = [Tags.CLUMSY])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ReturnOfBooleanExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(SyntaxViews.IF_STATEMENT, ::visitIfStatement)
    }

    private fun visitIfStatement(statement: IfStatement) {
        val elseBranch = statement.elseBranch
        if (!hasElsif(statement) && elseBranch != null) {
            val firstBoolean = booleanReturnedBy(statement.statementAstNodes)
            val secondBoolean = booleanReturnedBy(elseBranch.statementAstNodes)

            if (firstBoolean != null && secondBoolean != null
                    && firstBoolean.tokenValue != secondBoolean.tokenValue) {
                addIssue(statement, getLocalizedMessage())
            }
        }
    }

    private fun hasElsif(ifStatement: IfStatement): Boolean {
        return ifStatement.elsifBranches.isNotEmpty()
    }

    private fun booleanReturnedBy(statements: List<AstNode>): AstNode? {
        return extractBooleanValueFromReturn(statements.singleOrNull())
    }

    private fun extractBooleanValueFromReturn(node: AstNode?): AstNode? {
        if (node != null) {
            val child = node.firstChild
            if (child.typeIs(PlSqlGrammar.RETURN_STATEMENT)) {
                val expression = child.getFirstChildOrNull(PlSqlGrammar.LITERAL)

                return getBooleanLiteral(expression)
            }
        }
        return null
    }

    private fun getBooleanLiteral(expression: AstNode?): AstNode? {
        return expression?.getFirstChildOrNull(PlSqlGrammar.BOOLEAN_LITERAL)
    }

}
