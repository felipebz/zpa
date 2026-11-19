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
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.ColumnBinding

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class NotInWithNullCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IN_EXPRESSION, PlSqlGrammar.COMPARISON_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val subquery = when (node.type) {
            PlSqlGrammar.IN_EXPRESSION -> {
                if (!node.hasDirectChildren(PlSqlKeyword.NOT)) return
                node.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION)
                    ?: return checkList(node)
            }
            else -> notEqualsAll(node) ?: return
        }
        // `(a, b) NOT IN (…)`: a null in one column leaves the other comparison to decide.
        if (node.firstChild.hasDirectChildren(PlSqlPunctuator.COMMA) || negated(node)) return
        val column = nullableItem(subquery) ?: return
        addIssue(node, getLocalizedMessage(), column)
    }

    private fun checkList(node: AstNode) {
        if (negated(node)) return
        val values = node.children.filter { it.type !is PlSqlKeyword && it.type !is PlSqlPunctuator }.drop(1)
        if (values.any { CheckUtils.isNullLiteralOrEmptyString(it) }) addIssue(node, getLocalizedMessage("listMessage"))
    }

    /** The subquery of `x <> ALL (subquery)`, `!= ALL` or `^= ALL`. */
    private fun notEqualsAll(node: AstNode): AstNode? {
        val operator = node.getFirstChildOrNull(ConditionsGrammar.RELATIONAL_OPERATOR) ?: return null
        if (!operator.hasDirectChildren(PlSqlGrammar.NOTEQUALS_OPERATOR) || !operator.hasDirectChildren(PlSqlKeyword.ALL)) return null
        // The subquery is written in parentheses after the operator.
        return node.lastChild.takeIf { it.children.size == 3 }?.children?.get(1)?.takeIf { it.type === DmlGrammar.SELECT_EXPRESSION }
    }

    /** Whether a `NOT` applies to [node]: `NOT (x NOT IN (1, NULL))` is true for `x = 1`. */
    private fun negated(node: AstNode): Boolean {
        var current = node.parent
        while (current != null && current.type in CONNECTIVES) {
            if (current.type === PlSqlGrammar.NOT_EXPRESSION && current.hasDirectChildren(PlSqlKeyword.NOT)) return true
            current = current.parent
        }
        return false
    }

    /**
     * The column the single item of [subquery] reads, a column of a source of its one query block that may be null: the
     * metadata says it is nullable, or an outer join gives nulls for its source. Null when a top-level condition of
     * the block's `WHERE` rejects null for it, or when anything is not proven.
     */
    private fun nullableItem(subquery: AstNode): String? {
        val blocks = subquery.getChildren(DmlGrammar.QUERY_BLOCK)
        if (blocks.size != 1) return null
        val block = blocks.single()
        val item = block.getChildren(DmlGrammar.SELECT_COLUMN).singleOrNull()?.firstChild ?: return null
        val binding = bound(item, block) ?: return null
        val nullable = binding.column?.nullable?.orElse(false) == true || context.isOuterJoined(binding.source)
        if (!nullable) return null
        val where = block.getFirstChildOrNull(DmlGrammar.WHERE_CLAUSE)?.lastChild
        if (where != null && NullRejectingConditions.conjuncts(where).any { condition ->
                NullRejectingConditions.operands(condition).any { bound(it, block)?.let { other -> other.source == binding.source && other.name == binding.name } == true }
            }) return null
        return binding.name
    }

    private fun bound(node: AstNode, block: AstNode): ColumnBinding.Column? {
        if (node.type !== PlSqlGrammar.VARIABLE_NAME && node.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        val binding = context.columnBinding(node) as? ColumnBinding.Column ?: return null
        if (binding.exists != true || binding.source.node.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) !== block) return null
        return binding
    }

    private companion object {
        val CONNECTIVES = setOf(PlSqlGrammar.BRACKED_EXPRESSION, PlSqlGrammar.AND_EXPRESSION, PlSqlGrammar.OR_EXPRESSION, PlSqlGrammar.NOT_EXPRESSION)
    }
}
