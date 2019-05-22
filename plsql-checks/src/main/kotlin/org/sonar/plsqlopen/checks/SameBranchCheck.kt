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
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import java.util.*

@Rule(key = SameBranchCheck.CHECK_KEY, priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SameBranchCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val branches = collectStatementsFromBranches(node)
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
            addIssue(branch, getLocalizedMessage(CHECK_KEY), otherBranch.token.line)
                    .secondary(otherBranch, "Original")
        }
    }

    private fun collectStatementsFromBranches(node: AstNode): List<AstNode> {
        val statementsFromBranches = ArrayList<AstNode>()

        statementsFromBranches.add(getStatements(node))

        for (branch in node.getChildren(PlSqlGrammar.ELSIF_CLAUSE)) {
            statementsFromBranches.add(getStatements(branch))
        }

        val elseBranch = node.getFirstChild(PlSqlGrammar.ELSE_CLAUSE)
        if (elseBranch != null) {
            statementsFromBranches.add(getStatements(elseBranch))
        }

        return statementsFromBranches
    }

    private fun getStatements(node: AstNode): AstNode {
        return node.getFirstChild(PlSqlGrammar.STATEMENTS)
    }

    companion object {
        const val CHECK_KEY = "SameBranch"
    }

}
