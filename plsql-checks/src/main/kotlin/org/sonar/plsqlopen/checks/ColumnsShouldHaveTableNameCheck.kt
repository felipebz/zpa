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
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = ColumnsShouldHaveTableNameCheck.CHECK_KEY, priority = Priority.MAJOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ColumnsShouldHaveTableNameCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN)
    }

    override fun visitNode(node: AstNode) {
        var candidate = node.firstChild
        if (candidate.firstChild != null) {
            candidate = candidate.firstChild
        }

        val selectExpression = node.parent
        if (selectExpression.getFirstChild(DmlGrammar.FROM_CLAUSE).getChildren(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE).size > 1 &&
                candidate.typeIs(PlSqlGrammar.IDENTIFIER_NAME) &&
                !candidate.hasDirectChildren(PlSqlGrammar.NON_RESERVED_KEYWORD) &&
                semantic(candidate).symbol == null) {

            addIssue(candidate, getLocalizedMessage(CHECK_KEY), candidate.tokenOriginalValue)
        }
    }

    companion object {
        internal const val CHECK_KEY = "ColumnsShouldHaveTableName"
    }
}
