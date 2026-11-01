//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
