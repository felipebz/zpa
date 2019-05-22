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
import org.sonar.check.Priority
import org.sonar.check.Rule
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annotations.RuleInfo

@Rule(key = EmptyBlockCheck.CHECK_KEY, priority = Priority.MINOR, tags = [Tags.UNUSED])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class EmptyBlockCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.STATEMENTS_SECTION)
    }

    override fun visitNode(node: AstNode) {
        val statements = node.getFirstChild(PlSqlGrammar.STATEMENTS).getChildren(PlSqlGrammar.STATEMENT)
        if (statements.size == 1) {
            val nullStatementSelect = statements[0].getChildren(PlSqlGrammar.NULL_STATEMENT)
            if (nullStatementSelect.isNotEmpty()) {
                addLineIssue(getLocalizedMessage(CHECK_KEY), statements[0].tokenLine)
            }
        }
    }

    companion object {
        const val CHECK_KEY = "EmptyBlock"
    }

}
