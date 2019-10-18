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
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.annotations.*
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
        val topParent = node.parent.parent.parent
        if (topParent.typeIs(PlSqlGrammar.EXISTS_EXPRESSION)) {
            return
        }

        if (topParent.typeIs(PlSqlGrammar.CURSOR_DECLARATION)) {

            // TODO this is very complex and it probably can be simplified in the future
            val fetchDestination = semantic(topParent.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME))
                .symbol?.usages().orEmpty()
                .map { it.parent.parent }
                .filter { it.typeIs(PlSqlGrammar.FETCH_STATEMENT) }
                .map { it.getFirstChild(DmlGrammar.INTO_CLAUSE)?.getFirstChild(PlSqlGrammar.VARIABLE_NAME) }
                .firstOrNull()

            if (fetchDestination != null && semantic(fetchDestination).plSqlType == PlSqlType.ROWTYPE) {
                return
            }
        }

        var candidate = node.firstChild

        if (candidate.typeIs(PlSqlGrammar.OBJECT_REFERENCE)) {
            candidate = candidate.lastChild
        }

        if (candidate.typeIs(PlSqlPunctuator.MULTIPLICATION)) {
            val intoClause = node.parent.getFirstChild(DmlGrammar.INTO_CLAUSE)

            if (intoClause != null) {
                val variablesInInto = intoClause.getChildren(PlSqlGrammar.VARIABLE_NAME)
                val type = semantic(variablesInInto[0]).plSqlType
                if (variablesInInto.size == 1 && (type == PlSqlType.ROWTYPE || type == PlSqlType.ASSOCIATIVE_ARRAY)) {
                    return
                }
            }


            addIssue(node, getLocalizedMessage(CHECK_KEY))
        }
    }

    companion object {
        internal const val CHECK_KEY = "SelectAllColumns"
    }

}
