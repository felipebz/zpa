/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
import org.sonar.plsqlopen.asTree
import org.sonar.plsqlopen.sslr.IfStatement
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SameBranchCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val ifStatement = node.asTree<IfStatement>()
        val branches = collectStatementsFromBranches(ifStatement)
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

    private fun collectStatementsFromBranches(ifStatement: IfStatement): List<AstNode> {
        val statementsFromBranches = ArrayList<AstNode>()

        statementsFromBranches.add(ifStatement.statements.astNode)

        for (branch in ifStatement.elsifClauses) {
            statementsFromBranches.add(branch.statements.astNode)
        }

        val elseBranch = ifStatement.elseClause
        if (elseBranch != null) {
            statementsFromBranches.add(elseBranch.statements.astNode)
        }

        return statementsFromBranches
    }

}
