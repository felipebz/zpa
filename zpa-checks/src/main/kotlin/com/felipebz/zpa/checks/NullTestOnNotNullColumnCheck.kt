//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.ColumnBinding

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class NullTestOnNotNullColumnCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(ConditionsGrammar.BOOLEAN_TEST_CONDITION)
    }

    override fun visitNode(node: AstNode) {
        if (!node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || node.getFirstAncestorOrNull(DmlGrammar.WHERE_CLAUSE) == null) return
        val operand = node.firstChild
        if (operand.type !== PlSqlGrammar.VARIABLE_NAME && operand.type !== PlSqlGrammar.MEMBER_EXPRESSION) return
        val binding = context.columnBinding(operand) as? ColumnBinding.Column ?: return
        val column = binding.column?.takeIf { binding.exists == true } ?: return
        if (column.nullable.orElse(true) || context.outerJoined(binding.source)) return
        val name = column.name.originalSpelling.trim('"')
        if (node.hasDirectChildren(PlSqlKeyword.NOT)) {
            addIssue(node, getLocalizedMessage("alwaysTrueMessage"), name)
        } else {
            addIssue(node, getLocalizedMessage(), name)
        }
    }

}
