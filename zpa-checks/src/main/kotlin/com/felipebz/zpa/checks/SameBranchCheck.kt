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
