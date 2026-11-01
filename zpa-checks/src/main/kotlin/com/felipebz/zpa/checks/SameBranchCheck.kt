//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.syntax.IfStatement
import com.felipebz.zpa.api.syntax.SyntaxViews

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class SameBranchCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(SyntaxViews.IF_STATEMENT, ::visitIfStatement)
    }

    private fun visitIfStatement(statement: IfStatement) {
        val branches = collectStatementsFromBranches(statement)
        findSameBranches(branches)
    }

    private fun findSameBranches(branches: List<AstNode>) {
        for (i in 1 until branches.size) {
            checkBranch(branches, i)
        }
    }

    private fun checkBranch(branches: List<AstNode>, index: Int) {
        val branch = branches[index]
        val previousBranchIndex = index - 1
        val otherBranch = branches[previousBranchIndex]
        if (CheckUtils.equalNodes(otherBranch, branch)) {
            addIssue(branch, getLocalizedMessage(), otherBranch.token.line)
                    .secondary(otherBranch, "Original")
        }
    }

    private fun collectStatementsFromBranches(statement: IfStatement) = buildList {
        add(statement.statementsAstNode)
        statement.elsifBranches.forEach { add(it.statementsAstNode) }
        statement.elseBranch?.let { add(it.statementsAstNode) }
    }

}
