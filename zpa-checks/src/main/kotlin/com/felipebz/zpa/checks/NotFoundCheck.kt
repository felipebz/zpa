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
import com.felipebz.zpa.typeIs
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class NotFoundCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val parent = node.parent
        if (parent.typeIs(PlSqlGrammar.NOT_EXPRESSION) && node.numberOfChildren == 3) {

            val foundCandidate = node.lastChild
            val percentCandidate = foundCandidate.previousAstNode

            if (percentCandidate.typeIs(PlSqlPunctuator.MOD) && "FOUND" == foundCandidate.tokenValue) {
                addIssue(node, getLocalizedMessage())
            }
        }
    }

}
