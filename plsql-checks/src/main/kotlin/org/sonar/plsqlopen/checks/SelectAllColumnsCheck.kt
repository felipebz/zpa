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
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

@Rule(key = SelectAllColumnsCheck.CHECK_KEY, priority = Priority.MAJOR, tags = [Tags.PERFORMANCE])
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SelectAllColumnsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN)
    }

    override fun visitNode(node: AstNode) {
        if (node.parent.parent.`is`(PlSqlGrammar.EXISTS_EXPRESSION)) {
            return
        }

        var candidate = node.firstChild

        if (candidate.`is`(PlSqlGrammar.OBJECT_REFERENCE)) {
            candidate = candidate.lastChild
        }

        if (candidate.`is`(PlSqlPunctuator.MULTIPLICATION)) {
            val intoClause = node.parent.getFirstChild(DmlGrammar.INTO_CLAUSE)

            if (intoClause != null) {
                if (intoClause.firstChild.type === PlSqlKeyword.BULK) {
                    return
                }

                val variablesInInto = intoClause.getChildren(PlSqlGrammar.VARIABLE_NAME)
                if (variablesInInto.size == 1 && semantic(variablesInInto[0]).plSqlType === PlSqlType.ROWTYPE) {
                    return
                }
            }

            addLineIssue(getLocalizedMessage(CHECK_KEY), candidate.tokenLine)
        }
    }

    companion object {
        const val CHECK_KEY = "SelectAllColumns"
    }

}
