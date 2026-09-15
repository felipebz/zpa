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
