//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.*
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.ColumnBinding
import com.felipebz.zpa.api.semantics.ColumnSource
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.api.semantics.NameDeclarationKind

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ColumnHidesPlSqlNameCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_NAME)
    }

    override fun visitNode(node: AstNode) {
        if (node.parent.type === PlSqlGrammar.MEMBER_EXPRESSION) return
        val other = otherSide(node) ?: return
        val declaration = (context.nameBinding(node) as? NameBinding.ColumnOrLocal)?.declaration ?: return
        if (declaration.kind != NameDeclarationKind.PARAMETER && declaration.kind != NameDeclarationKind.VARIABLE) return
        val column = provenColumn(node) ?: return
        if (!other(column)) return
        val key = if (declaration.kind == NameDeclarationKind.PARAMETER) "message" else "variableMessage"
        addIssue(node, getLocalizedMessage(key), column.name, written(column.source), node.tokenOriginalValue)
    }

    private class ProvenColumn(val source: ColumnSource, val name: String)

    private fun provenColumn(name: AstNode): ProvenColumn? = when (val binding = context.columnBinding(name)) {
        is ColumnBinding.Column -> if (binding.exists == true) ProvenColumn(binding.source, binding.name) else null
        is ColumnBinding.QueryColumn -> if (binding.item != null) ProvenColumn(binding.source, binding.name) else null
        else -> null
    }

    private fun readColumn(node: AstNode): ProvenColumn? {
        val name = if (node.type === PlSqlGrammar.OBJECT_REFERENCE && node.children.size == 1) node.firstChild else node
        if (name.type !== PlSqlGrammar.VARIABLE_NAME && name.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        return when (val binding = context.columnBinding(name)) {
            is ColumnBinding.Column -> ProvenColumn(binding.source, binding.name)
            is ColumnBinding.QueryColumn -> ProvenColumn(binding.source, binding.name)
            else -> null
        }
    }

    private fun same(a: ProvenColumn?, b: ProvenColumn) =
        a != null && a.source.node === b.source.node && a.name.equals(b.name, ignoreCase = true)

    /**
     * When [name] is an operand of a comparison in a condition (`WHERE`, `HAVING`, a join's `ON`, `START WITH`,
     * `CONNECT BY`, the `ON` of a `MERGE`), or the whole value of a `SET`: whether the other side reads a given column.
     * Of two unqualified names, the right one answers, so the comparison is reported once. Null in any other place.
     */
    private fun otherSide(name: AstNode): ((ProvenColumn) -> Boolean)? {
        val parent = name.parent
        if (parent.type === DmlGrammar.UPDATE_COLUMN && parent.lastChild === name) {
            val target = parent.firstChild
            return { column -> same(readColumn(target), column) || targetNames(target, column) }
        }
        if (parent.type !== PlSqlGrammar.COMPARISON_EXPRESSION || !parent.hasDirectChildren(ConditionsGrammar.RELATIONAL_OPERATOR) || !inCondition(
                parent
            )
        ) return null
        val other = when (name) {
            parent.firstChild -> parent.lastChild
            parent.lastChild -> parent.firstChild
            else -> return null
        }
        if (name === parent.firstChild && other.type === PlSqlGrammar.VARIABLE_NAME) return null
        return { column -> same(readColumn(other), column) }
    }

    /** Whether the `SET` target [target] is the column [column] of the statement's own target. */
    private fun targetNames(target: AstNode, column: ProvenColumn): Boolean {
        val written = target.tokens.lastOrNull { it.type !is PlSqlPunctuator } ?: return false
        if (!written.value.equals(column.name, ignoreCase = true)) return false
        val statement =
            target.getFirstAncestorOrNull(DmlGrammar.UPDATE_EXPRESSION, DmlGrammar.MERGE_EXPRESSION) ?: return false
        val own =
            if (statement.type === DmlGrammar.UPDATE_EXPRESSION) statement.getFirstChildOrNull(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
            else statement.children.firstOrNull { it.type === DmlGrammar.TABLE_REFERENCE || it.type === DmlGrammar.DML_SUBQUERY_TARGET }
        return own != null && (column.source.node === own || generateSequence(column.source.node.parentOrNull) { it.parentOrNull }.any { it === own })
    }

    /** Whether [comparison] is a condition: the nearest clause around it is a condition, or the `ON` of a `MERGE`. */
    private fun inCondition(comparison: AstNode): Boolean {
        var child = comparison
        var parent = comparison.parentOrNull
        while (parent != null) {
            when (parent.type) {
                in CONDITIONS -> return true
                in STOPS -> return false
                DmlGrammar.MERGE_EXPRESSION -> return child.previousSiblingOrNull?.type === PlSqlPunctuator.LPARENTHESIS &&
                    child.previousSiblingOrNull?.previousSiblingOrNull?.type === PlSqlKeyword.ON
            }
            child = parent
            parent = parent.parentOrNull
        }
        return false
    }

    private fun written(source: ColumnSource): String {
        val node = source.node
        val clause = if (node.type === DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE) node else node.parent
        clause.getFirstChildOrNull(DmlGrammar.ALIAS)?.let { return it.lastToken.originalValue }
        return source.alias ?: node.tokens.joinToString("") { it.originalValue }
    }

    private companion object {
        val CONDITIONS = setOf(
            DmlGrammar.WHERE_CLAUSE, DmlGrammar.HAVING_CLAUSE, DmlGrammar.ON_OR_USING_EXPRESSION,
            DmlGrammar.START_WITH_CLAUSE, DmlGrammar.CONNECT_BY_CLAUSE
        )
        val STOPS = setOf(
            DmlGrammar.SELECT_COLUMN, DmlGrammar.ORDER_BY_CLAUSE, DmlGrammar.GROUP_BY_CLAUSE, DmlGrammar.INTO_CLAUSE,
            DmlGrammar.VALUES_CLAUSE, DmlGrammar.RETURNING_INTO_CLAUSE, DmlGrammar.QUERY_BLOCK
        )
    }
}
