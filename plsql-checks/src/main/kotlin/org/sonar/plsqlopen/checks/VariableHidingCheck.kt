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

@Rule(key = VariableHidingCheck.CHECK_KEY, priority = Priority.MAJOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class VariableHidingCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val name = identifier.tokenOriginalValue

        val scope = context.symbolTable?.getScopeForSymbol(identifier)
        if (scope != null) {
            val symbols = scope.getSymbolsAcessibleInScope(name)

            if (symbols.size > 1) {
                val originalVariable = symbols.last.declaration()

                if (originalVariable != identifier) {
                    addIssue(identifier, getLocalizedMessage(CHECK_KEY), name, originalVariable.tokenLine)
                            .secondary(originalVariable, "Original")
                }
            }
        }
    }

    companion object {
        const val CHECK_KEY = "VariableHiding"
    }
}
