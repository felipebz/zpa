/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
class CommitRollbackCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.COMMIT_STATEMENT, PlSqlGrammar.ROLLBACK_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val scope = context.currentScope

        var outerScope = scope
        while (outerScope?.outer != null) {
            outerScope = outerScope.outer
        }

        val isRollbackToSavepoint = node.typeIs(PlSqlGrammar.ROLLBACK_STATEMENT) && node.hasDirectChildren(PlSqlKeyword.TO)

        val currentScopeIsAutonomousTransaction = scope?.isAutonomousTransaction ?: false
        val isInsideABlockStatement = outerScope?.tree?.typeIs(PlSqlGrammar.BLOCK_STATEMENT) ?: false

        if (!isRollbackToSavepoint && !currentScopeIsAutonomousTransaction && !isInsideABlockStatement) {
            addLineIssue(getLocalizedMessage(), node.tokenLine, node.tokenValue)
        }
    }

}
