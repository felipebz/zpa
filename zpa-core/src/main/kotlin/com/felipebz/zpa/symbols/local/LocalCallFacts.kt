//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.project.DatatypeFamilies
import com.felipebz.zpa.api.project.OverloadRank
import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName

/** The datatype of a literal actual, as Oracle types it for overload purposes; [typeName] is null for `NULL`. */
internal enum class LocalLiteral(
    val typeName: String?,
    /** The exact type Oracle's choice among compatible overloads sees; null when it is not known. */
    val rank: OverloadRank? = typeName?.let { DatatypeFamilies.rankOfTypeName(it) }
) {
    NULL(null),

    /** With a decimal point or an exponent. */
    NUMBER("NUMBER"),

    /** Digits only, within the PLS_INTEGER range: Oracle prefers a PLS_INTEGER formal for it. */
    INTEGER("NUMBER", OverloadRank.PLS_INTEGER),

    /** Digits only, beyond the PLS_INTEGER range: its preferred formal is not known. */
    LARGE_INTEGER("NUMBER", null),
    BINARY_DOUBLE("BINARY_DOUBLE"),
    BINARY_FLOAT("BINARY_FLOAT"),

    /** Every character literal, the empty string `''` included. */
    CHAR("CHAR"),
    NCHAR("NCHAR"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    /** With or without a time zone, which the literal's text tells and this does not read. */
    TIMESTAMP("TIMESTAMP", null),
    INTERVAL_YEAR_TO_MONTH("INTERVAL YEAR TO MONTH"),
    INTERVAL_DAY_TO_SECOND("INTERVAL DAY TO SECOND")
}

/** The form of an actual that is neither a literal nor a name, when this model types it. */
internal sealed interface LocalActualForm {
    /** A condition: `IN`, a comparison or `BETWEEN`, `IS …`, `LIKE`. */
    data object Predicate : LocalActualForm

    /** `a || b || …`, each operand an actual of its own. */
    class Concatenation(val operands: List<LocalActual>) : LocalActualForm

    /** `TREAT(x AS t)`, with the name of the type written. */
    class Treat(val type: QualifiedName) : LocalActualForm

    /** Members written after an element (`v(i).m`) or a `TREAT`, without parentheses after them. */
    class Members(val base: LocalActual, val members: List<OracleIdentifier>) : LocalActualForm

    /**
     * A method called on the value of a constructor, an element or a `TREAT` (`t(…).m(…)`, `v(i).a.m(…)`): the members
     * before the method, the method's name, and the labels of its actuals (null for a positional one).
     */
    class Method(val base: LocalActual, val members: List<OracleIdentifier>, val method: OracleIdentifier, val labels: List<OracleIdentifier?>) : LocalActualForm

    /**
     * An element of a call's result, `f(…)(i)`: [base] is the call, then one index per argument list after its own.
     * Oracle reads the first list as the call's actuals, even for a function without parameters (PLS-00306 for `f(1)`).
     */
    class Element(val base: LocalActual, val indexes: Int) : LocalActualForm

    /** `-x` or `+x`. */
    class Signed(val operand: LocalActual) : LocalActualForm

    /** `c%ISOPEN`, `c%FOUND`, `c%NOTFOUND` or `c%ROWCOUNT` of any cursor, `SQL` included: [rowCount] for the last. */
    class CursorAttribute(val rowCount: Boolean) : LocalActualForm
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
    val isCall: Boolean = false,
    /** For an actual that is neither a literal nor a name: its form, when modeled. */
    val form: LocalActualForm? = null
) {
    val isName: Boolean get() = nameLine > 0

    override fun toString() = (label?.let { "$it => " } ?: "") +
        (literal?.name ?: if (isName) (if (isCall) "call" else "name") + "@$nameLine:$nameColumn" else "expression")
}

/** The actuals of one call, keyed by the position of the called name's first segment. */
internal class LocalCall(
    val line: Int,
    val column: Int,
    /**
     * The call's own actuals, in written order, also for `f(…)(i)`, whose later lists index its result; null when the
     * form is not modeled (a later list with several actuals, `f(1)(2, 3)`).
     */
    val actuals: List<LocalActual>?
) {
    override fun toString() = "call@$line:$column${actuals ?: "(not modeled)"}"
}

/** Collects the calls of a parsed file as AST-detached [LocalCall] facts, in document order. */
internal object LocalCallCollector {
    /** Conditions, which PL/SQL types as BOOLEAN whatever their operands. */
    private val PREDICATES = setOf(
        PlSqlGrammar.IN_EXPRESSION, PlSqlGrammar.COMPARISON_EXPRESSION, ConditionsGrammar.BOOLEAN_TEST_CONDITION, ConditionsGrammar.LIKE_CONDITION
    )

    fun collect(root: AstNode): List<LocalCall> {
        val out = ArrayList<LocalCall>()
        visit(root, out)
        return out
    }

    private fun visit(node: AstNode, out: MutableList<LocalCall>) {
        when {
            node.type === PlSqlGrammar.METHOD_CALL -> {
                val lists = node.getChildren(PlSqlGrammar.ARGUMENTS)
                val actuals = if (lists.size == 1 || indexes(lists)) lists.first().getChildren(PlSqlGrammar.ARGUMENT).map(::actual) else null
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
        if (argument.hasDirectChildren(PlSqlKeyword.DISTINCT, PlSqlKeyword.UNIQUE)) return LocalActual(label, null, 0, 0)
        return expression(label, argument.lastChild)
    }

    private fun expression(label: OracleIdentifier?, expression: AstNode): LocalActual {
        if (expression.type === PlSqlGrammar.LITERAL) return LocalActual(label, literal(expression), 0, 0)
        if (expression.type in PREDICATES) return LocalActual(label, null, 0, 0, form = LocalActualForm.Predicate)
        if (expression.type === PlSqlGrammar.CONCATENATION_EXPRESSION) {
            val operands = expression.children.filter { it.type !== PlSqlGrammar.CONCATENATION_OPERATOR }.map { expression(null, it) }
            return LocalActual(label, null, 0, 0, form = LocalActualForm.Concatenation(operands))
        }
        treatType(expression)?.let { return LocalActual(label, null, 0, 0, form = LocalActualForm.Treat(it)) }
        if (expression.type === PlSqlGrammar.OBJECT_REFERENCE) return LocalActual(label, null, 0, 0, form = members(expression))
        if (expression.type === PlSqlGrammar.METHOD_CALL) {
            val lists = expression.getChildren(PlSqlGrammar.ARGUMENTS)
            val called = name(expression.firstChild) ?: return LocalActual(label, null, 0, 0)
            if (lists.size == 1) return LocalActual(label, null, called.token.line, called.token.column, isCall = true)
            if (!indexes(lists)) return LocalActual(label, null, 0, 0)
            val call = LocalActual(null, null, called.token.line, called.token.column, isCall = true)
            return LocalActual(label, null, 0, 0, form = LocalActualForm.Element(call, lists.size - 1))
        }
        if (expression.type === PlSqlGrammar.UNARY_EXPRESSION && expression.children.size == 2 &&
            expression.firstChild.type.let { it === PlSqlPunctuator.MINUS || it === PlSqlPunctuator.PLUS }
        ) return LocalActual(label, null, 0, 0, form = LocalActualForm.Signed(expression(null, expression.lastChild)))
        cursorAttribute(expression)?.let { return LocalActual(label, null, 0, 0, form = it) }
        val name = name(expression) ?: return LocalActual(label, null, 0, 0)
        return LocalActual(label, null, name.token.line, name.token.column)
    }

    /**
     * `v(i).m…`, `t(…).m…` or `TREAT(x AS t).m…`: the base, a dot, then names, or names ending in one method call.
     * Null for anything else (an attribute, a second element, a method with several argument lists).
     */
    private fun members(reference: AstNode): LocalActualForm? {
        val children = reference.children
        if (children.size != 3 || children[1].type !== PlSqlPunctuator.DOT) return null
        val base = children[0]
        val baseActual = when {
            base.type === PlSqlGrammar.METHOD_CALL -> expression(null, base).takeIf { it.isCall || it.form is LocalActualForm.Element } ?: return null
            treatType(base) != null -> expression(null, base)
            else -> return null
        }
        val after = children[2]
        if (after.type === PlSqlGrammar.METHOD_CALL) {
            val lists = after.getChildren(PlSqlGrammar.ARGUMENTS)
            if (lists.size != 1) return null
            val names = nameSegments(after.firstChild)?.map(LocalSyntax::identifier) ?: return null
            val labels = lists.single().getChildren(PlSqlGrammar.ARGUMENT).map { argument ->
                if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) LocalSyntax.identifier(argument.firstChild) else null
            }
            return LocalActualForm.Method(baseActual, names.dropLast(1), names.last(), labels)
        }
        val names = nameSegments(after) ?: return null
        return LocalActualForm.Members(baseActual, names.map(LocalSyntax::identifier))
    }

    /** Whether every argument list after the first holds one positional actual: each indexes the result before it. */
    private fun indexes(lists: List<AstNode>): Boolean = lists.size > 1 && lists.drop(1).all { list ->
        list.getChildren(PlSqlGrammar.ARGUMENT).singleOrNull()?.hasDirectChildren(PlSqlPunctuator.ASSOCIATION) == false
    }

    /** `name%ISOPEN`, `name%FOUND`, `name%NOTFOUND` or `name%ROWCOUNT`, else null. */
    private fun cursorAttribute(expression: AstNode): LocalActualForm.CursorAttribute? {
        val children = expression.children
        if (expression.type !== PlSqlGrammar.MEMBER_EXPRESSION || children.first().type !== PlSqlGrammar.VARIABLE_NAME ||
            children.size < 3 || children[children.size - 2].type !== PlSqlPunctuator.MOD || expression.hasDirectChildren(PlSqlPunctuator.REMOTE)
        ) return null
        return when (children.last().tokenValue.uppercase()) {
            "ISOPEN", "FOUND", "NOTFOUND" -> LocalActualForm.CursorAttribute(rowCount = false)
            "ROWCOUNT" -> LocalActualForm.CursorAttribute(rowCount = true)
            else -> null
        }
    }

    /** The identifier nodes of a plain or qualified name, or null for anything else. */
    private fun nameSegments(expression: AstNode): List<AstNode>? = name(expression)?.let { node ->
        if (node.type === PlSqlGrammar.VARIABLE_NAME) listOf(node.firstChild)
        else listOf(node.firstChild.firstChild) + node.children.drop(1).filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
    }

    /** The type named in `TREAT(x AS t)`, when it is a plain or qualified name. */
    private fun treatType(expression: AstNode): QualifiedName? {
        if (expression.type !== SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION) return null
        val treat = expression.getFirstChildOrNull(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION) ?: return null
        val custom = treat.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.getFirstChildOrNull(PlSqlGrammar.CUSTOM_DATATYPE) ?: return null
        if (custom.children.size != 1) return null
        val name = name(custom.firstChild) ?: return null
        val identifiers = if (name.type === PlSqlGrammar.VARIABLE_NAME) listOf(name.firstChild)
        else listOf(name.firstChild.firstChild) + name.children.drop(1).filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
        return QualifiedName(identifiers.map(LocalSyntax::identifier))
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
                else -> when {
                    !text.all { it.isDigit() } -> LocalLiteral.NUMBER
                    (text.trimStart('0').ifEmpty { "0" }.toIntOrNull() ?: -1) >= 0 -> LocalLiteral.INTEGER
                    else -> LocalLiteral.LARGE_INTEGER
                }
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
