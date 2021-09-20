/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import com.sonar.sslr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import java.util.regex.Pattern

@Rule(priority = Priority.MINOR)
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class VariableNameCheck : AbstractBaseCheck() {

    @RuleProperty(key = "regexp", defaultValue = DEFAULT_REGEXP)
    var regexp = DEFAULT_REGEXP

    private val pattern: Pattern by lazy {
        Pattern.compile(regexp)
    }

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val name = identifier.tokenOriginalValue

        if (!pattern.matcher(name).matches()) {
            addIssue(identifier, getLocalizedMessage(), name, regexp)
        }
    }

    companion object {
        private const val DEFAULT_REGEXP = "[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?"
    }

}
