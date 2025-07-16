/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.asTree
import com.felipebz.zpa.sslr.RaiseStatement
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.symbols.Symbol
import java.util.*

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
class UnhandledUserDefinedExceptionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val statement = node.asTree<RaiseStatement>()
        val identifier = statement.exception
                ?: return

        val identifierName = identifier.tokenValue
        val symbols = context.currentScope?.getSymbolsAcessibleInScope(identifierName) ?: ArrayDeque()

        if (symbols.isNotEmpty()) {
            val checkException = exceptionShouldBeChecked(symbols.first)

            if (checkException && !isHandled(identifierName)) {
                addIssue(node, getLocalizedMessage(), identifierName)
            }
        }
    }

    private fun exceptionShouldBeChecked(exceptionDeclaration: Symbol): Boolean {
        return exceptionDeclaration.scope.type !in PACKAGE_SPEC_OR_BODY
    }

    private fun isHandled(identifierName: String): Boolean {
        var outerScope = context.currentScope
        do {
            val scopeNode = outerScope?.tree

            val statements = scopeNode?.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)
            val exceptionHandlers = statements?.getFirstChildOrNull(PlSqlGrammar.EXCEPTION_HANDLERS)
            if (statements != null && exceptionHandlers != null) {
                for (handler in exceptionHandlers.getChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
                    if (handler.hasDirectChildren(PlSqlKeyword.OTHERS) && !handler.hasDescendant(PlSqlKeyword.SQLERRM)) {
                        return true
                    }

                    for (exceptionName in handler.getChildren(PlSqlGrammar.VARIABLE_NAME)) {
                        if (exceptionName.tokenValue == identifierName) {
                            return true
                        }
                    }
                }
            }

            outerScope = outerScope?.outer
        } while (outerScope != null)

        return false
    }

    companion object {
        val PACKAGE_SPEC_OR_BODY = arrayOf(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
    }

}
