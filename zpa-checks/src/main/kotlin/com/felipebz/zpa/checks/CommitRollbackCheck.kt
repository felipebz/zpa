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
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
class CommitRollbackCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.COMMIT_STATEMENT, PlSqlGrammar.ROLLBACK_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        if (node.typeIs(PlSqlGrammar.ROLLBACK_STATEMENT) && node.hasDirectChildren(PlSqlKeyword.TO)) {
            return
        }

        if (node.getFirstAncestorOrNull(PlSqlGrammar.ANONYMOUS_BLOCK) != null) {
            return
        }

        if (!mayBeAutonomous(node)) {
            addLineIssue(getLocalizedMessage(), node.tokenLine, node.tokenValue)
        }
    }

    private fun mayBeAutonomous(node: AstNode): Boolean =
        generateSequence(node.getFirstAncestorOrNull(*UNITS)) { it.getFirstAncestorOrNull(*UNITS) }.any { unit ->
            declaresAutonomousTransaction(unit) || PlSqlStructure.hasKeptConditionalBranchInDeclarations(unit)
        }

    private fun declaresAutonomousTransaction(unit: AstNode): Boolean =
        unit.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)
            ?.getChildren(PlSqlGrammar.PRAGMA_DECLARATION)
            ?.any { it.hasDirectChildren(PlSqlGrammar.AUTONOMOUS_TRANSACTION_PRAGMA) } == true

    private companion object {
        val UNITS = arrayOf<AstNodeType>(
            PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.TYPE_CONSTRUCTOR, PlSqlGrammar.SIMPLE_DML_TRIGGER,
            PlSqlGrammar.INSTEAD_OF_DML_TRIGGER, PlSqlGrammar.COMPOUND_DML_TRIGGER, PlSqlGrammar.SYSTEM_TRIGGER
        )
    }

}
