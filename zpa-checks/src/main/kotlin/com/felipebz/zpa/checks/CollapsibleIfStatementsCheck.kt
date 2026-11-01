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

@Rule(priority = Priority.MAJOR, tags = [Tags.CLUMSY])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class CollapsibleIfStatementsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(SyntaxViews.IF_STATEMENT, ::visitIfStatement)
    }

    private fun visitIfStatement(statement: IfStatement) {
        val nestedIf = singleIfChild(statement)
        if (nestedIf != null && !hasElseOrElsif(statement) &&
                !nestedIf.hasDirectChildren(PlSqlGrammar.ELSIF_CLAUSE, PlSqlGrammar.ELSE_CLAUSE)) {
            addIssue(nestedIf, getLocalizedMessage())
        }
    }

    private fun hasElseOrElsif(statement: IfStatement): Boolean {
        return statement.elsifBranches.isNotEmpty() || statement.elseBranch != null
    }

    private fun singleIfChild(statement: IfStatement): AstNode? {
        return statement.statementAstNodes.singleOrNull()
            ?.getFirstChildOrNull(PlSqlGrammar.IF_STATEMENT)
    }

}
