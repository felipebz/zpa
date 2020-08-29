/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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
import org.sonar.plsqlopen.isOf
import org.sonar.plsqlopen.sslr.NullStatement
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = EmptyBlockCheck.CHECK_KEY, priority = Priority.MINOR, tags = [Tags.UNUSED])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class EmptyBlockCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.STATEMENTS_SECTION)
    }

    override fun visitNode(node: AstNode) {
        if (context.currentScope?.isOverridingMember == true) {
            return
        }

        val statements = node.getFirstChild(PlSqlGrammar.STATEMENTS).getChildren(PlSqlGrammar.STATEMENT)
        if (statements.size == 1) {
            val statement = statements[0]
            if (statement.isOf<NullStatement>()) {
                addIssue(statement, getLocalizedMessage(CHECK_KEY))
            }
        }
    }

    companion object {
        internal const val CHECK_KEY = "EmptyBlock"
    }

}
