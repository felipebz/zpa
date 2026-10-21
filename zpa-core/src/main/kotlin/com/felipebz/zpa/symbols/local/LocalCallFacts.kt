/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.OracleIdentifier

/** The datatype of a literal actual, as Oracle types it for overload purposes; [typeName] is null for `NULL`. */
internal enum class LocalLiteral(val typeName: String?) {
    NULL(null),
    NUMBER("NUMBER"),
    BINARY_DOUBLE("BINARY_DOUBLE"),
    BINARY_FLOAT("BINARY_FLOAT"),

    /** Every character literal, the empty string `''` included. */
    CHAR("CHAR"),
    NCHAR("NCHAR"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    TIMESTAMP("TIMESTAMP"),
    INTERVAL_YEAR_TO_MONTH("INTERVAL YEAR TO MONTH"),
    INTERVAL_DAY_TO_SECOND("INTERVAL DAY TO SECOND")
}

/** One written actual of a call: its notation and its form. */
internal class LocalActual(
    /** The formal named by `name => value`; null for a positional actual. */
    val label: OracleIdentifier?,
    /** Set for a literal actual. */
    val literal: LocalLiteral?,
    /**
     * For a plain or qualified name, or the called name of `name(…)`, the line of its first segment (the start of its
     * [LocalReference]); 0 otherwise.
     */
    val nameLine: Int,
    val nameColumn: Int,
    /** The actual is `name(…)`: a call or an element access, with its own [LocalCall] at the same position. */
    val isCall: Boolean = false
) {
    val isName: Boolean get() = nameLine > 0

    override fun toString() = (label?.let { "$it => " } ?: "") +
        (literal?.name ?: if (isName) (if (isCall) "call" else "name") + "@$nameLine:$nameColumn" else "expression")
}

/** The actuals of one call, keyed by the position of the called name's first segment. */
internal class LocalCall(
    val line: Int,
    val column: Int,
    /** In written order; null when the form is not modeled (several argument lists, `f(1)(2)`). */
    val actuals: List<LocalActual>?
) {
    override fun toString() = "call@$line:$column${actuals ?: "(not modeled)"}"
}

/** Collects the calls of a parsed file as AST-detached [LocalCall] facts, in document order. */
internal object LocalCallCollector {

    fun collect(root: AstNode): List<LocalCall> {
        val out = ArrayList<LocalCall>()
        visit(root, out)
        return out
    }

    private fun visit(node: AstNode, out: MutableList<LocalCall>) {
        when {
            node.type === PlSqlGrammar.METHOD_CALL -> {
                val lists = node.getChildren(PlSqlGrammar.ARGUMENTS)
                val actuals = if (lists.size == 1) lists.single().getChildren(PlSqlGrammar.ARGUMENT).map(::actual) else null
                out += LocalCall(node.firstChild.token.line, node.firstChild.token.column, actuals)
            }
            node.type === PlSqlGrammar.CALL_STATEMENT && !node.hasDirectChildren(PlSqlGrammar.METHOD_CALL) ->
                node.getFirstChildOrNull(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
                    ?.let { out += LocalCall(it.token.line, it.token.column, emptyList()) }
        }
        node.children.forEach { visit(it, out) }
    }

    private fun actual(argument: AstNode): LocalActual {
        val label = if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) LocalSyntax.identifier(argument.firstChild) else null
        val expression = argument.lastChild
        if (argument.hasDirectChildren(PlSqlKeyword.DISTINCT, PlSqlKeyword.UNIQUE)) return LocalActual(label, null, 0, 0)
        if (expression.type === PlSqlGrammar.LITERAL) return LocalActual(label, literal(expression), 0, 0)
        if (expression.type === PlSqlGrammar.METHOD_CALL) {
            // `c(1)(2)`, `h()(1)`: an element of a result, an expression this model does not type.
            if (expression.getChildren(PlSqlGrammar.ARGUMENTS).size != 1) return LocalActual(label, null, 0, 0)
            val called = name(expression.firstChild) ?: return LocalActual(label, null, 0, 0)
            return LocalActual(label, null, called.token.line, called.token.column, isCall = true)
        }
        val name = name(expression) ?: return LocalActual(label, null, 0, 0)
        return LocalActual(label, null, name.token.line, name.token.column)
    }

    /** A plain or qualified name: names only, no attribute and no database link. */
    private fun name(expression: AstNode): AstNode? =
        when (expression.type) {
            PlSqlGrammar.VARIABLE_NAME -> expression.takeIf { it.hasDirectChildren(PlSqlGrammar.IDENTIFIER_NAME) }
            // `a.b`: names only, no attribute and no database link.
            PlSqlGrammar.MEMBER_EXPRESSION -> expression.takeIf { member ->
                member.firstChild.type === PlSqlGrammar.VARIABLE_NAME && member.firstChild.hasDirectChildren(PlSqlGrammar.IDENTIFIER_NAME) &&
                    !member.hasDirectChildren(PlSqlPunctuator.MOD, PlSqlPunctuator.REMOTE)
            }
            else -> null
        }

    /** As [com.felipebz.zpa.api.project.ProjectArgumentType] lists literal evidence; null when not classified. */
    private fun literal(literal: AstNode): LocalLiteral? {
        val node = literal.firstChild
        val text = node.token.originalValue
        return when (node.type) {
            PlSqlGrammar.NULL_LITERAL -> LocalLiteral.NULL
            PlSqlGrammar.BOOLEAN_LITERAL -> LocalLiteral.BOOLEAN
            PlSqlGrammar.NUMERIC_LITERAL -> when (text.last()) {
                'd', 'D' -> LocalLiteral.BINARY_DOUBLE
                'f', 'F' -> LocalLiteral.BINARY_FLOAT
                else -> LocalLiteral.NUMBER
            }
            PlSqlGrammar.FLOATING_POINT_LITERAL ->
                if (text.startsWith("BINARY_FLOAT", ignoreCase = true)) LocalLiteral.BINARY_FLOAT else LocalLiteral.BINARY_DOUBLE
            PlSqlGrammar.CHARACTER_LITERAL -> if (text.startsWith("n", ignoreCase = true)) LocalLiteral.NCHAR else LocalLiteral.CHAR
            PlSqlGrammar.INTERVAL_LITERAL ->
                if (node.hasDirectChildren(PlSqlGrammar.INTERVAL_YEAR_TO_MONTH_LITERAL)) LocalLiteral.INTERVAL_YEAR_TO_MONTH
                else LocalLiteral.INTERVAL_DAY_TO_SECOND
            PlSqlTokenType.DATE_LITERAL -> LocalLiteral.DATE
            PlSqlTokenType.TIMESTAMP_LITERAL -> LocalLiteral.TIMESTAMP
            else -> null
        }
    }
}
