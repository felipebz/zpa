//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.api.semantics.RecordOrigin
import com.felipebz.zpa.api.semantics.ValueCategory
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR, tags = [Tags.PERFORMANCE])
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class SelectAllColumnsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN)
    }

    override fun visitNode(node: AstNode) {
        val topParent = node.parent.parent.parent
        if (topParent.typeIs(PlSqlGrammar.EXISTS_EXPRESSION)) {
            return
        }

        if (topParent.typeIs(PlSqlGrammar.CURSOR_DECLARATION) && isFetchedAsRow(topParent)) {
            return
        }

        var candidate = node.firstChild

        if (candidate.typeIs(PlSqlGrammar.OBJECT_REFERENCE)) {
            candidate = candidate.lastChild
        }

        if (candidate.typeIs(PlSqlPunctuator.MULTIPLICATION)) {
            val intoClause = node.parent.getFirstChildOrNull(DmlGrammar.INTO_CLAUSE)

            if (intoClause != null && intoMayBeRow(intoClause)) {
                return
            }

            addIssue(node, getLocalizedMessage())
        }
    }

    private fun isFetchedAsRow(cursor: AstNode): Boolean {
        val name = cursor.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        var root = cursor
        while (true) root = root.parentOrNull ?: break
        return root.getDescendants(PlSqlGrammar.FETCH_STATEMENT).any { fetch ->
            val fetched = fetch.getFirstChildOrNull(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
            val into = fetch.getFirstChildOrNull(DmlGrammar.INTO_CLAUSE)
            fetched != null && into != null && mayName(fetched, name) && intoMayBeRow(into)
        }
    }

    private fun mayName(fetched: AstNode, name: AstNode): Boolean {
        if (fetched.type === PlSqlGrammar.MEMBER_EXPRESSION) return sameName(fetched.lastChild, name)
        return when (val binding = context.nameBinding(fetched)) {
            is NameBinding.Local -> binding.declaration.let { it.fileId == null && it.line == name.token.line && it.column == name.token.column }
            is NameBinding.Unknown, is NameBinding.Unavailable -> sameName(fetched, name)
            else -> false
        }
    }

    private fun sameName(written: AstNode, name: AstNode) = written.tokenValue == name.tokenValue

    private fun intoMayBeRow(intoClause: AstNode): Boolean {
        val destinations = intoClause.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.METHOD_CALL)
        return destinations.size == 1 && mayBeRow(destinations[0])
    }

    private fun mayBeRow(destination: AstNode): Boolean = when (destination.type) {
        PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> when (val category = context.valueCategory(destination)) {
            is ValueCategory.Collection -> mayBeRowElement(category.element)
            else -> mayBeRowElement(category)
        }
        PlSqlGrammar.METHOD_CALL -> {
            val collection = destination.firstChild
            val category = if (collection.type === PlSqlGrammar.VARIABLE_NAME || collection.type === PlSqlGrammar.MEMBER_EXPRESSION) {
                context.valueCategory(collection)
            } else null
            when (category) {
                is ValueCategory.Collection -> mayBeRowElement(category.element)
                is ValueCategory.Unknown, is ValueCategory.Unavailable -> true
                else -> false
            }
        }
        else -> false
    }

    private fun mayBeRowElement(category: ValueCategory) = when (category) {
        is ValueCategory.Record -> category.origin == RecordOrigin.ROWTYPE
        is ValueCategory.Unknown, is ValueCategory.Unavailable -> true
        else -> false
    }

}
