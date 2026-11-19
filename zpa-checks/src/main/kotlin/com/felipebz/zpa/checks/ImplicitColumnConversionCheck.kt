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
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.ColumnBinding
import com.felipebz.zpa.api.semantics.ScalarFamily
import com.felipebz.zpa.api.semantics.ValueCategory

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG, Tags.PERFORMANCE], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ImplicitColumnConversionCheck : AbstractBaseCheck() {

    private enum class Target { NUMBER, DATE }

    override fun init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION, PlSqlGrammar.IN_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        if (node.getFirstAncestorOrNull(*CLAUSES) == null) return
        val operands = node.children.filter { it.type !is PlSqlKeyword && it.type !is PlSqlPunctuator && it.type !== ConditionsGrammar.RELATIONAL_OPERATOR }
        when {
            node.type === PlSqlGrammar.IN_EXPRESSION -> {
                if (!node.hasDirectChildren(PlSqlKeyword.IN) || node.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) return
                report(node, operands.first(), operands.drop(1))
            }
            node.hasDirectChildren(PlSqlKeyword.BETWEEN) -> report(node, operands.first(), operands.drop(1))
            else -> {
                val operator = node.getFirstChildOrNull(ConditionsGrammar.RELATIONAL_OPERATOR) ?: return
                // `x = ANY (…)` and the like compare with every value of a list or a subquery: not modeled.
                if (operator.children.size != 1 || operands.size != 2) return
                if (!report(node, operands[0], listOf(operands[1]))) report(node, operands[1], listOf(operands[0]))
            }
        }
    }

    /** Reports [node] when [column] is a character column and one of [values] is a number or a date; whether it did. */
    private fun report(node: AstNode, column: AstNode, values: List<AstNode>): Boolean {
        val name = characterColumn(column) ?: return false
        val target = values.firstNotNullOfOrNull(::targetOf) ?: return false
        addIssue(node, getLocalizedMessage(if (target == Target.NUMBER) "message" else "dateMessage"), name)
        return true
    }

    /** The name of the column [node] directly reads, when the metadata gives it a character type. */
    private fun characterColumn(node: AstNode): String? {
        if (node.type !== PlSqlGrammar.VARIABLE_NAME && node.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        val binding = context.columnBinding(node) as? ColumnBinding.Column ?: return null
        val column = binding.column?.takeIf { binding.exists == true } ?: return null
        val datatype = column.datatype.orElse(null)?.takeIf { !it.typeOwner.isPresent } ?: return null
        if (datatype.name.substringBefore('(').trim().uppercase() !in CHARACTER_TYPES) return null
        return column.name.originalSpelling.trim('"')
    }

    /** What [value] is when it is proven to be a number or a datetime: a literal, or a name of that type. */
    private fun targetOf(value: AstNode): Target? {
        val literal = if (value.type === PlSqlGrammar.LITERAL) value.firstChild else value
        return when (literal.type) {
            PlSqlGrammar.NUMERIC_LITERAL -> Target.NUMBER
            PlSqlTokenType.DATE_LITERAL, PlSqlTokenType.TIMESTAMP_LITERAL -> Target.DATE
            PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> columnTarget(value) ?: when ((context.valueCategory(value) as? ValueCategory.Scalar)?.family) {
                ScalarFamily.NUMERIC -> Target.NUMBER
                ScalarFamily.DATETIME -> Target.DATE
                else -> null
            }
            else -> null
        }
    }

    /** What a column of another source is, by the type the metadata gives it; null when it is no proven column of such a type. */
    private fun columnTarget(value: AstNode): Target? {
        val binding = context.columnBinding(value) as? ColumnBinding.Column ?: return null
        val datatype = binding.column?.takeIf { binding.exists == true }?.datatype?.orElse(null)?.takeIf { !it.typeOwner.isPresent } ?: return null
        val name = datatype.name.substringBefore('(').trim().uppercase()
        return when {
            name in NUMERIC_TYPES -> Target.NUMBER
            name == "DATE" || name.startsWith("TIMESTAMP") -> Target.DATE
            else -> null
        }
    }

    private companion object {
        val NUMERIC_TYPES = setOf("NUMBER", "FLOAT", "INTEGER", "BINARY_FLOAT", "BINARY_DOUBLE")
        val CLAUSES = arrayOf(
            DmlGrammar.WHERE_CLAUSE, DmlGrammar.HAVING_CLAUSE, DmlGrammar.ON_OR_USING_EXPRESSION,
            DmlGrammar.START_WITH_CLAUSE, DmlGrammar.CONNECT_BY_CLAUSE
        )
        val CHARACTER_TYPES = setOf("VARCHAR2", "CHAR", "NVARCHAR2", "NCHAR")
    }
}
