//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.RecordOrigin
import com.felipebz.zpa.api.semantics.RecordRelation
import com.felipebz.zpa.api.semantics.ValueCategory

@Rule(priority = Priority.CRITICAL, tags = [Tags.CONVENTION, Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class InsertWithoutColumnsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SINGLE_TABLE_INSERT)
    }

    override fun visitNode(node: AstNode) {
        // SET assigns each column by name and BY NAME maps the subquery columns by alias.
        if (node.hasDirectChildren(DmlGrammar.INSERT_SET_CLAUSE) || node.hasDirectChildren(PlSqlKeyword.NAME)) {
            return
        }

        if (!node.hasDescendant(DmlGrammar.INSERT_COLUMNS)) {
            val valuesClause = node.getFirstChildOrNull(DmlGrammar.VALUES_CLAUSE)

            if (valuesClause != null && !valuesClause.hasDirectChildren(PlSqlPunctuator.LPARENTHESIS) && mayBeRow(valuesClause.lastChild)) {
                return
            }

            if (node.parent.parent.parent.type === PlSqlGrammar.FORALL_STATEMENT) {
                return
            }

            addIssue(node, getLocalizedMessage())
        }
    }

    private fun mayBeRow(value: AstNode): Boolean = when (value.type) {
        PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> mayBeRow(context.valueCategory(value))
        // `v(i)`: an element of a collection, or a call whose result is not known.
        PlSqlGrammar.METHOD_CALL -> mayBeElementRow(value.firstChild)
        else -> false
    }

    private fun mayBeRow(category: ValueCategory) = when (category) {
        is ValueCategory.Record -> category.origin == RecordOrigin.ROWTYPE && category.relation != RecordRelation.QueryColumns
        ValueCategory.ObjectInstance, is ValueCategory.Unknown, is ValueCategory.Unavailable -> true
        is ValueCategory.Collection, is ValueCategory.Scalar, ValueCategory.CursorVariable -> false
    }

    private fun mayBeElementRow(name: AstNode): Boolean {
        if (name.type !== PlSqlGrammar.VARIABLE_NAME && name.type !== PlSqlGrammar.MEMBER_EXPRESSION) return false
        return when (val category = context.valueCategory(name)) {
            is ValueCategory.Collection -> mayBeRow(category.element)
            is ValueCategory.Unknown, is ValueCategory.Unavailable -> true
            else -> false
        }
    }

}
