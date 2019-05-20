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
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.util.*

@Rule(key = UnhandledUserDefinedExceptionCheck.CHECK_KEY, priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
class UnhandledUserDefinedExceptionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.VARIABLE_NAME)
                ?: return

        val identifierName = identifier.tokenOriginalValue
        val symbols = context.currentScope?.getSymbolsAcessibleInScope(identifierName) ?: ArrayDeque()

        if (symbols.isNotEmpty()) {
            val checkException = exceptionShouldBeChecked(symbols.first)

            if (checkException && !isHandled(identifierName)) {
                addIssue(node, getLocalizedMessage(CHECK_KEY), identifierName)
            }
        }
    }

    private fun exceptionShouldBeChecked(exceptionDeclaration: Symbol): Boolean {
        val scopeOfDeclaration = exceptionDeclaration.scope().tree()
        val isPackage = scopeOfDeclaration?.typeIs(PACKAGE_SPEC_OR_BODY) ?: false
        return !isPackage
    }

    private fun isHandled(identifierName: String): Boolean {
        var outerScope = context.currentScope
        do {
            val scopeNode = outerScope?.tree()

            val statements = scopeNode?.getFirstChild(PlSqlGrammar.STATEMENTS_SECTION)
            if (statements != null) {
                for (handler in statements.getChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
                    if (handler.hasDirectChildren(PlSqlKeyword.OTHERS) && !handler.hasDescendant(PlSqlKeyword.SQLERRM)) {
                        return true
                    }

                    for (exceptionName in handler.getChildren(PlSqlGrammar.VARIABLE_NAME)) {
                        if (exceptionName.tokenOriginalValue.equals(identifierName, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }

            outerScope = outerScope?.outer()
        } while (outerScope != null)

        return false
    }

    companion object {
        const val CHECK_KEY = "UnhandledUserDefinedException"

        val PACKAGE_SPEC_OR_BODY = arrayOf(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
    }

}
