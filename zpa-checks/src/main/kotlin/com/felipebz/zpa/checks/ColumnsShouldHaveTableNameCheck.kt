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
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ColumnsShouldHaveTableNameCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN)
    }

    override fun visitNode(node: AstNode) {
        var candidate = node.firstChild
        if (candidate.hasChildren()) {
            candidate = candidate.firstChild
        }

        val selectExpression = node.parentOrNull
        if (selectExpression != null &&
            selectExpression.hasDirectChildren(DmlGrammar.FROM_CLAUSE) &&
            selectExpression.getFirstChild(DmlGrammar.FROM_CLAUSE).getChildren(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE).size > 1 &&
            candidate.typeIs(PlSqlGrammar.IDENTIFIER_NAME) &&
            !candidate.hasDirectChildren(PlSqlGrammar.NON_RESERVED_KEYWORD) &&
            semantic(candidate).symbol == null) {

            addIssue(candidate, getLocalizedMessage(), candidate.tokenOriginalValue)
        }
    }
}
