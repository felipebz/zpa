/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR, tags = [Tags.CONFUSING])
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
            addIssue(node, getLocalizedMessage())
        }
    }

}

