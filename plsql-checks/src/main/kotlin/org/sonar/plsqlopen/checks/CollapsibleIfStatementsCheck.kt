/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(priority = Priority.MAJOR, tags = [Tags.CLUMSY])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class CollapsibleIfStatementsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val ifStatement = node.asTree<IfStatement>()
        val singleIfChild = singleIfChild(ifStatement)
        if (singleIfChild != null && !hasElseOrElsif(ifStatement) && !hasElseOrElsif(singleIfChild)) {
            addIssue(singleIfChild, getLocalizedMessage())
        }
    }

    private fun hasElseOrElsif(ifStatement: IfStatement): Boolean {
        return ifStatement.elsifClauses.isNotEmpty() || ifStatement.elseClause != null
    }

    private fun singleIfChild(ifStatement: IfStatement): IfStatement? {
        val statements = ifStatement.statements
        if (statements.size == 1) {
            val nestedIf = statements[0].getChildren(PlSqlGrammar.IF_STATEMENT)
            if (nestedIf.size == 1) {
                return nestedIf[0].asTree()
            }
        }
        return null
    }

}
