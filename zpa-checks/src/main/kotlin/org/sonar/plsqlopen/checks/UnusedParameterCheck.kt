/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.util.regex.Pattern

@Rule(priority = Priority.MAJOR, tags = [Tags.UNUSED])
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnusedParameterCheck : AbstractBaseCheck() {

    @RuleProperty(key = "ignoreMethods", defaultValue = "")
    var ignoreMethods = ""

    private val ignoreRegex: Pattern? by lazy {
        if ("" != ignoreMethods) {
            Pattern.compile(ignoreMethods)
        } else null
    }

    override fun leaveFile(node: AstNode) {
        val scopes = context.symbolTable.scopes
        for (scope in scopes) {
            // is overriding something?
            if (scope.isOverridingMember) {
                continue
            }

            val scopeNode = scope.tree ?: continue

            // ignore procedure/function specification and overriding members
            if (scope.type in DECLARATION_OR_CONSTRUCTOR) {
                // is specification?
                if (!scopeNode.hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION)) {
                    continue
                }
            }

            // cursor declaration (without implementation)
            if (scope.type == PlSqlGrammar.CURSOR_DECLARATION && !scopeNode.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) {
                continue
            }

            // ignore methods by name
            if (scope.type in PROCEDURE_OR_FUNCTION && ignoreRegex != null) {
                val matchesRegex = ignoreRegex?.matcher(scope.identifier)?.matches() ?: false
                if (matchesRegex) {
                    continue
                }
            }

            checkScope(scope)
        }
    }

    private fun checkScope(scope: Scope) {
        val symbols = scope.getSymbols(Symbol.Kind.PARAMETER)
        for (symbol in symbols) {

            // SELF parameter in type members
            if (scope.outer?.type == PlSqlGrammar.CREATE_TYPE_BODY && symbol.name.equals("self", ignoreCase = true)) {
                continue
            }


            if (symbol.usages.isEmpty()) {
                val parent = checkNotNull(symbol.declaration.parent)
                addIssue(parent, getLocalizedMessage(), symbol.name)
            }

        }
    }

    companion object {
        val DECLARATION_OR_CONSTRUCTOR =
            arrayOf(PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,
                PlSqlGrammar.TYPE_CONSTRUCTOR)
        val PROCEDURE_OR_FUNCTION =
            arrayOf(PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION)
    }

}
