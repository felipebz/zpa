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
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annotations.RuleInfo

@Rule(key = TooManyRowsHandlerCheck.CHECK_KEY, priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class TooManyRowsHandlerCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.EXCEPTION_HANDLER)
    }

    override fun visitNode(node: AstNode) {
        // is a TOO_MANY_ROWS handler
        val exceptions = node.getChildren(PlSqlGrammar.VARIABLE_NAME)

        for (exception in exceptions) {
            val child = exception.firstChild

            if (child.typeIs(PlSqlGrammar.IDENTIFIER_NAME) && "TOO_MANY_ROWS".equals(child.tokenValue, ignoreCase = true)) {
                // and have only one NULL_STATEMENT
                val children = node.getFirstChild(PlSqlGrammar.STATEMENTS).children
                if (children.size == 1 && children[0].firstChild.typeIs(PlSqlGrammar.NULL_STATEMENT)) {
                    addLineIssue(getLocalizedMessage(CHECK_KEY), node.tokenLine)
                }
            }
        }
    }

    companion object {
        const val CHECK_KEY = "TooManyRowsHandler"
    }
}

