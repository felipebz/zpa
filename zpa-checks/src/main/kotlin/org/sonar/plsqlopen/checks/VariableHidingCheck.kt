/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
import org.sonar.plsqlopen.asSemantic
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class VariableHidingCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val name = identifier.tokenValue

        val scope = identifier.asSemantic().symbol?.scope
        if (scope != null) {
            val symbols = scope.getSymbolsAcessibleInScope(name, Symbol.Kind.VARIABLE, Symbol.Kind.PARAMETER)

            if (symbols.size > 1) {
                val originalVariable = symbols.last.declaration

                if (originalVariable != identifier) {
                    addIssue(identifier, getLocalizedMessage(), name, originalVariable.tokenLine)
                            .secondary(originalVariable, "Original")
                }
            }
        }
    }

}
