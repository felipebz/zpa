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
import com.felipebz.zpa.isOf
import com.felipebz.zpa.sslr.NullStatement
import com.felipebz.zpa.typeIs
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class TooManyRowsHandlerCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.EXCEPTION_HANDLER)
    }

    override fun visitNode(node: AstNode) {
        // is a TOO_MANY_ROWS handler
        val exceptions = node.getChildren(PlSqlGrammar.VARIABLE_NAME)

        for (exception in exceptions) {
            val child = exception.firstChild

            if (child.typeIs(PlSqlGrammar.IDENTIFIER_NAME) && "TOO_MANY_ROWS".equals(child.tokenValue, ignoreCase = true)) {
                // and have only one NULL_STATEMENT
                val children = node.getFirstChild(PlSqlGrammar.STATEMENTS).children
                if (children.size == 1 && children[0].isOf<NullStatement>()) {
                    addIssue(node, getLocalizedMessage())
                }
            }
        }
    }

}

