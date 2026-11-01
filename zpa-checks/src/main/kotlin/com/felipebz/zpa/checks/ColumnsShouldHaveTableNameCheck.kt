//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
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
            !isStandaloneQueryValue(candidate) &&
            isColumn(candidate)) {

            addIssue(candidate, getLocalizedMessage(), candidate.tokenOriginalValue)
        }
    }

    private fun isColumn(identifier: AstNode): Boolean {
        val name = identifier.parentOrNull?.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return false
        return (context.nameBinding(name) as? NameBinding.NothingLocal)?.sqlColumnPossible == true
    }

    private fun isStandaloneQueryValue(node: AstNode): Boolean {
        if (node.tokenOriginalValue.startsWith('"')) return false
        return when (node.tokenValue) {
            "ROWNUM", "LEVEL", "CONNECT_BY_ISCYCLE", "CONNECT_BY_ISLEAF",
            "SYSDATE", "SYSTIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP", "LOCALTIMESTAMP",
            "DBTIMEZONE", "SESSIONTIMEZONE", "USER", "UID" -> true
            else -> false
        }
    }
}
