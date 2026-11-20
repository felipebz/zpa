//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.*
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class NotASelectedExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val queryBlock = node.getFirstChild(DmlGrammar.QUERY_BLOCK)
        if (!queryBlock.children[1].typeIs(PlSqlKeyword.DISTINCT) || !node.hasDirectChildren(DmlGrammar.ORDER_BY_CLAUSE)) {
            return
        }
        val columns = queryBlock.getChildren(DmlGrammar.SELECT_COLUMN)
        // With `*` the selected columns are not known.
        if (columns.any { isStar(it.firstChild) }) {
            return
        }
        val query = Query(columns, sourceNames(queryBlock))
        for (item in node.getFirstChild(DmlGrammar.ORDER_BY_CLAUSE).getChildren(DmlGrammar.ORDER_BY_ITEM)) {
            val expression = item.firstChild
            if (!query.accepts(expression)) {
                addIssue(skipVariableName(expression), getLocalizedMessage())
            }
        }
    }

    private fun isStar(expression: AstNode): Boolean {
        val last = if (expression.typeIs(PlSqlGrammar.OBJECT_REFERENCE)) expression.lastChild else expression
        return last.typeIs(PlSqlPunctuator.MULTIPLICATION)
    }

    private fun skipVariableName(node: AstNode): AstNode =
        if (node.typeIs(PlSqlGrammar.VARIABLE_NAME)) node.firstChild else node

    /** The aliases and table names of the sources the query block reads, normalized. */
    private fun sourceNames(queryBlock: AstNode): Set<String> {
        val from = queryBlock.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE) ?: return emptySet()
        val names = HashSet<String>()
        for (source in from.getDescendants(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)) {
            if (source.getFirstAncestorOrNull(DmlGrammar.FROM_CLAUSE) !== from) continue
            source.getFirstChildOrNull(DmlGrammar.ALIAS)?.let { names += identifier(it.lastToken.originalValue) }
            source.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE)?.let { names += identifier(it.lastToken.originalValue) }
        }
        return names
    }

    private inner class Query(columns: List<AstNode>, private val sources: Set<String>) {
        private val selected = columns.map { CheckUtils.skipParenthesis(it.firstChild) }
        private val aliases = columns.filter { it.numberOfChildren > 1 }.map { identifier(it.lastToken.originalValue) }.toSet()

        /** Whether Oracle accepts [expression] as an `ORDER BY` item of this `SELECT DISTINCT`. */
        fun accepts(expression: AstNode): Boolean {
            val node = CheckUtils.skipParenthesis(expression)
            if (selected.any { same(it, node) }) return true
            return when (node.type) {
                PlSqlGrammar.LITERAL -> true
                PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> !isColumn(node)
                // The name of the function is not a column: only its arguments are checked.
                PlSqlGrammar.METHOD_CALL -> node.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)
                    ?.getChildren(PlSqlGrammar.ARGUMENT).orEmpty()
                    .all { accepts(it.lastChild) }
                else -> node.children.filter { it.hasChildren() || isName(it) }.all { accepts(it) }
            }
        }

        private fun isName(node: AstNode) = node.type === PlSqlGrammar.VARIABLE_NAME || node.type === PlSqlGrammar.MEMBER_EXPRESSION

        /**
         * Whether [name] is a column of the query, not an alias of the select list, a bind variable, a PL/SQL name, a
         * value such as `SYSDATE`, or a member of a package (`pkg.value`).
         */
        private fun isColumn(name: AstNode): Boolean {
            if (name.token.type === PlSqlPunctuator.COLON || name.tokenValue.startsWith(":")) return false
            val parts = parts(name) ?: return false
            if (parts.size > 1) return parts[parts.size - 2] in sources
            val single = identifier(name.tokenOriginalValue)
            if (single in aliases || !name.tokenOriginalValue.startsWith('"') && single in VALUES) return false
            val variable = name.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return false
            return context.nameBinding(variable) is NameBinding.NothingLocal
        }

        /**
         * Whether [first] and [second] are the same expression as Oracle compares them: unquoted names without regard
         * to case, a column with or without the name of its source, parentheses, spacing and comments aside, and the two
         * operands of `+` and `*` in either order.
         */
        fun same(first: AstNode, second: AstNode): Boolean {
            val a = CheckUtils.skipParenthesis(first)
            val b = CheckUtils.skipParenthesis(second)
            if (isName(a) && isName(b)) sameName(a, b)?.let { return it }
            if (a.type !== b.type) return false
            if (!a.hasChildren() || !b.hasChildren()) return !a.hasChildren() && !b.hasChildren() && sameToken(a, b)
            val left = a.children
            val right = b.children
            if (left.size == right.size && left.indices.all { same(left[it], right[it]) }) return true
            return isCommutative(a) && isCommutative(b) && left[1].tokenValue == right[1].tokenValue &&
                same(left[0], right[2]) && same(left[2], right[0])
        }

        private fun isCommutative(node: AstNode) =
            (node.type === PlSqlGrammar.ADDITIVE_EXPRESSION || node.type === PlSqlGrammar.MULTIPLICATIVE_EXPRESSION) &&
                node.numberOfChildren == 3 && (node.children[1].tokenValue == "+" || node.children[1].tokenValue == "*")

        /** Null when either is not a plain dotted name: the nodes are then compared as other expressions. */
        private fun sameName(a: AstNode, b: AstNode): Boolean? {
            val left = parts(a) ?: return null
            val right = parts(b) ?: return null
            if (left.last() != right.last()) return false
            val leftQualifier = left.dropLast(1)
            val rightQualifier = right.dropLast(1)
            // `label` and `r.label` name the same column of the source `r`.
            if (leftQualifier.isEmpty() || rightQualifier.isEmpty()) {
                return (leftQualifier + rightQualifier).all { it in sources }
            }
            return leftQualifier == rightQualifier
        }

        /** The segments of a plain dotted name, normalized; null for anything else (a call, an attribute, a link). */
        private fun parts(name: AstNode): List<String>? = when (name.type) {
            PlSqlGrammar.VARIABLE_NAME -> listOf(identifier(name.tokenOriginalValue))
            PlSqlGrammar.MEMBER_EXPRESSION -> name.children.filter { it.type !== PlSqlPunctuator.DOT }.map { part ->
                if (part.type !== PlSqlGrammar.VARIABLE_NAME && part.type !== PlSqlGrammar.IDENTIFIER_NAME) return null
                identifier(part.tokenOriginalValue)
            }
            else -> null
        }

        /** Names as Oracle compares them, string literals by their exact text, anything else without regard to case. */
        private fun sameToken(a: AstNode, b: AstNode): Boolean = when {
            a.token.type === GenericTokenType.IDENTIFIER || a.type === PlSqlGrammar.IDENTIFIER_NAME ->
                identifier(a.tokenOriginalValue) == identifier(b.tokenOriginalValue)
            a.token.type === PlSqlTokenType.STRING_LITERAL || b.token.type === PlSqlTokenType.STRING_LITERAL ->
                a.tokenOriginalValue == b.tokenOriginalValue
            else -> a.tokenValue.equals(b.tokenValue, ignoreCase = true)
        }
    }

    private companion object {
        val VALUES = setOf(
            "ROWNUM", "LEVEL", "CONNECT_BY_ISCYCLE", "CONNECT_BY_ISLEAF", "SYSDATE", "SYSTIMESTAMP", "CURRENT_DATE",
            "CURRENT_TIMESTAMP", "LOCALTIMESTAMP", "DBTIMEZONE", "SESSIONTIMEZONE", "USER", "UID"
        )

        /** An identifier as Oracle compares it: a quoted one exactly, an unquoted one in uppercase. */
        fun identifier(text: String): String =
            if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) text.substring(1, text.length - 1) else text.uppercase()
    }
}
