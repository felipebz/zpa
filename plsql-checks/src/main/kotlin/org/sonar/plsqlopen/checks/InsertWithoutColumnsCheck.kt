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
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

@Rule(priority = Priority.CRITICAL, tags = [Tags.CONVENTION, Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class InsertWithoutColumnsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SINGLE_TABLE_INSERT)
    }

    override fun visitNode(node: AstNode) {
        if (!node.hasDescendant(DmlGrammar.INSERT_COLUMNS)) {
            val valuesClause = node.lastChild

            if (valuesClause.typeIs(DmlGrammar.VALUES_CLAUSE) && semantic(valuesClause.lastChild).plSqlType === PlSqlType.ROWTYPE) {
                return
            }

            if (node.parent.parent.parent.type === PlSqlGrammar.FORALL_STATEMENT) {
                return
            }

            addIssue(node, getLocalizedMessage())
        }
    }

}
