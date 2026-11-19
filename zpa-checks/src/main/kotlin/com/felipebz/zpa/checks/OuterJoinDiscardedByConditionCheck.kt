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
import com.felipebz.zpa.api.semantics.ColumnSource

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class OuterJoinDiscardedByConditionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.QUERY_BLOCK)
    }

    override fun visitNode(node: AstNode) {
        val from = node.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE) ?: return
        node.getFirstChildOrNull(DmlGrammar.WHERE_CLAUSE)?.lastChild?.let { where ->
            conjuncts(where).forEach { condition -> check(condition, node) { context.isOuterJoined(it) } }
        }
        for (join in from.getDescendants(DmlGrammar.INNER_CROSS_JOIN_CLAUSE)) {
            if (join.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) !== node) continue
            val on = join.getFirstChildOrNull(DmlGrammar.ON_OR_USING_EXPRESSION)
                ?.takeIf { it.firstChild.type === PlSqlKeyword.ON } ?: continue
            conjuncts(on.lastChild).forEach { condition -> check(condition, node) { nullableBefore(it, join) } }
        }
    }

    private fun check(condition: AstNode, block: AstNode, nullable: (ColumnSource) -> Boolean) {
        if (condition.hasDescendant(PlSqlGrammar.OUTER_JOIN_PLUS_SIGN)) return
        for (operand in nullRejectingOperands(condition)) {
            if (operand.type !== PlSqlGrammar.VARIABLE_NAME && operand.type !== PlSqlGrammar.MEMBER_EXPRESSION) continue
            val source = when (val binding = context.columnBinding(operand)) {
                is ColumnBinding.Column -> binding.source
                is ColumnBinding.QueryColumn -> binding.source
                else -> continue
            }
            if (source.node.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) !== block || repeatedAlias(
                    source,
                    block
                ) || !nullable(source)
            ) continue
            addIssue(condition, getLocalizedMessage(), written(source))
            return
        }
    }

    /**
     * Whether another source of [block] has the alias of [source]. Oracle reads such a qualifier as the source nearest
     * to where it is written, which the binding does not tell apart: the condition is left alone.
     */
    private fun repeatedAlias(source: ColumnSource, block: AstNode): Boolean {
        val alias = source.alias ?: return false
        val from = block.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE) ?: return false
        return from.getDescendants(DmlGrammar.ALIAS).count {
            it.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === block && it.lastToken.value.equals(
                alias,
                ignoreCase = true
            )
        } > 1
    }

    private fun written(source: ColumnSource): String {
        val node = source.node
        val clause = if (node.type === DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE) node else node.parent
        clause.getFirstChildOrNull(DmlGrammar.ALIAS)?.let { return it.lastToken.originalValue }
        return source.alias ?: node.tokens.joinToString("") { it.originalValue }
    }

    /** The operands of [condition] a null makes it never true, when it is a condition that rejects null. */
    private fun nullRejectingOperands(condition: AstNode): List<AstNode> = when (condition.type) {
        PlSqlGrammar.COMPARISON_EXPRESSION -> {
            val operator = condition.getFirstChildOrNull(ConditionsGrammar.RELATIONAL_OPERATOR)
            when {
                // `x <> ALL (…)` is true for a null x when the list is empty.
                operator != null -> if (operator.children.size == 1) listOf(
                    condition.firstChild,
                    condition.lastChild
                ) else emptyList()

                // `x NOT BETWEEN a AND b` is `x < a OR x > b`: a null bound leaves the other comparison to decide.
                condition.hasDirectChildren(PlSqlKeyword.BETWEEN) ->
                    if (condition.hasDirectChildren(PlSqlKeyword.NOT)) listOf(condition.firstChild) else condition.children.filter { it.type !is PlSqlKeyword }
                else -> emptyList()
            }
        }

        ConditionsGrammar.LIKE_CONDITION -> condition.children.filter { it.type !is PlSqlKeyword }.take(2)
        PlSqlGrammar.IN_EXPRESSION ->
            if (condition.hasDescendant(DmlGrammar.SELECT_EXPRESSION)) emptyList() else listOf(condition.firstChild)

        ConditionsGrammar.BOOLEAN_TEST_CONDITION ->
            if (condition.hasDirectChildren(PlSqlKeyword.NOT) && condition.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) listOf(
                condition.firstChild
            )
            else emptyList()

        else -> emptyList()
    }

    /** The top-level conjuncts of [expression]: itself, or the operands of its `AND` chain, through parentheses. */
    private fun conjuncts(expression: AstNode): List<AstNode> = when {
        expression.type === PlSqlGrammar.AND_EXPRESSION -> expression.children.filter { it.type !== PlSqlKeyword.AND }
            .flatMap(::conjuncts)

        expression.type === PlSqlGrammar.BRACKED_EXPRESSION && expression.children.size == 3 -> conjuncts(expression.children[1])
        else -> listOf(expression)
    }

    /**
     * Whether [source] is on the nullable side of an outer join written before [join] in its join list: its part is a
     * `LEFT` or `FULL` join, or `OUTER APPLY`, or a `RIGHT` or `FULL` join follows its part before [join].
     */
    private fun nullableBefore(source: ColumnSource, join: AstNode): Boolean {
        val list = join.parent.children
        val part = list.firstOrNull { it === source.node || source.node.hasAncestor(it) } ?: return false
        val start = list.indexOf(part)
        val end = list.indexOf(join)
        if (start >= end) return false
        if (part.type === DmlGrammar.OUTER_JOIN_CLAUSE && outerJoinType(part) in RIGHT_SIDE_NULLS) return true
        if (part.type === DmlGrammar.INNER_CROSS_JOIN_CLAUSE && part.hasDirectChildren(PlSqlKeyword.OUTER)) return true
        return list.subList(start + 1, end)
            .any { it.type === DmlGrammar.OUTER_JOIN_CLAUSE && outerJoinType(it) in LEFT_SIDE_NULLS }
    }

    private fun outerJoinType(join: AstNode) = join.getFirstChildOrNull(DmlGrammar.OUTER_JOIN_TYPE)?.firstChild?.type

    private fun AstNode.hasAncestor(ancestor: AstNode): Boolean =
        generateSequence(parentOrNull) { it.parentOrNull }.any { it === ancestor }

    private companion object {
        val RIGHT_SIDE_NULLS = setOf(PlSqlKeyword.LEFT, PlSqlKeyword.FULL)
        val LEFT_SIDE_NULLS = setOf(PlSqlKeyword.RIGHT, PlSqlKeyword.FULL)
    }
}
