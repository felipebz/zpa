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
import org.sonar.check.RuleProperty
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.util.regex.Pattern

@Rule(key = UnusedParameterCheck.CHECK_KEY, priority = Priority.MAJOR, tags = [Tags.UNUSED])
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
        val scopes = context.symbolTable?.scopes ?: emptySet()
        for (scope in scopes) {
            val scopeNode = scope.tree()
                    ?: continue

            // ignore procedure/function specification and overriding members
            if (scope.tree()?.typeIs(DECLARATION_OR_CONSTRUCTOR) == true) {
                // is specification?
                if (!scopeNode.hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION)) {
                    continue
                }

                // is overriding something?
                val inheritanceClause = scopeNode.parent.getFirstChild(PlSqlGrammar.INHERITANCE_CLAUSE)
                if (inheritanceClause != null && inheritanceClause.firstChild.type !== PlSqlKeyword.NOT &&
                        inheritanceClause.hasDirectChildren(PlSqlKeyword.OVERRIDING)) {
                    continue
                }
            }

            // cursor declaration (without implementation)
            if (scopeNode.typeIs(PlSqlGrammar.CURSOR_DECLARATION) && !scopeNode.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) {
                continue
            }

            // ignore methods by name
            if (scopeNode.typeIs(PROCEDURE_OR_FUNCTION) &&
                ignoreRegex != null) {
                val matchesRegex = ignoreRegex?.matcher(scope.identifier())?.matches() ?: false
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
            if (symbol.usages().isEmpty()) {
                addIssue(symbol.declaration().parent, getLocalizedMessage(CHECK_KEY),
                        symbol.declaration().tokenOriginalValue)
            }
        }
    }

    companion object {
        const val CHECK_KEY = "UnusedParameter"
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
