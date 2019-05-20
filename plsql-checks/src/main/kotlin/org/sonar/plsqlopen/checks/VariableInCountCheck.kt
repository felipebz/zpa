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
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.matchers.MethodMatcher

@Rule(key = VariableInCountCheck.CHECK_KEY, priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class VariableInCountCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL)
    }

    override fun visitNode(node: AstNode) {
        val count = MethodMatcher.create().name("count").addParameter()

        if (!node.parent.typeIs(DmlGrammar.SELECT_COLUMN) || !count.matches(node)) {
            return
        }

        checkArguments(node, count.getArgumentsValues(node))
    }

    private fun checkArguments(currentNode: AstNode, arguments: List<AstNode>) {
        if (arguments.size != 1) {
            return
        }

        val value = arguments[0]
        val symbol = semantic(value).symbol
        if (symbol != null) {
            addIssue(currentNode, getLocalizedMessage(CHECK_KEY), value.tokenOriginalValue)
        }
    }

    companion object {
        const val CHECK_KEY = "VariableInCount"
    }

}
