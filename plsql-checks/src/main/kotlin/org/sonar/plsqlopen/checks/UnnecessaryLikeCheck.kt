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
import org.sonar.plugins.plsqlopen.api.ConditionsGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = UnnecessaryLikeCheck.CHECK_KEY, priority = Priority.MINOR, tags = [Tags.CONFUSING])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnnecessaryLikeCheck : AbstractBaseCheck() {

    private val escapeRegex = Regex("[%_]")

    override fun init() {
        subscribeTo(ConditionsGrammar.LIKE_CONDITION)
    }

    override fun visitNode(node: AstNode) {
        val escapeChar = if (node.hasDescendant(PlSqlKeyword.ESCAPE) &&
            node.lastChild.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL))
            node.lastChild.tokenValue.trim('\'')
        else
            ""

        val regex = if (escapeChar.isEmpty()) escapeRegex else Regex("[^${Regex.escape(escapeChar)}][%_]")

        val likeNode = node.getFirstChild(PlSqlKeyword.LIKE).nextSibling
        if (likeNode.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL) && !likeNode.tokenValue.contains(regex)) {
            addIssue(node, getLocalizedMessage(CHECK_KEY))
        }
    }

    companion object {
        internal const val CHECK_KEY = "UnnecessaryLikeCheck"
    }

}

