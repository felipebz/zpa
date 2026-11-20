//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.*
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.dynamicsql.DynamicSqlHole
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.api.semantics.*
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.collections.ArrayDeque

@Rule(priority = Priority.CRITICAL, tags = [Tags.SECURITY], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("15min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ParameterInDynamicSqlCheck : AbstractBaseCheck() {

    private val reported = Collections.newSetFromMap(IdentityHashMap<AstNode, Boolean>())

    /** The variable and parameter declarations of the file by the position of their name, built on first use. */
    private var declarations: Map<Long, AstNode>? = null

    override fun init() {
        subscribeTo(PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT, PlSqlGrammar.OPEN_FOR_STATEMENT, PlSqlGrammar.METHOD_CALL)
    }

    override fun visitFile(node: AstNode) {
        reported.clear()
        declarations = null
    }

    override fun visitNode(node: AstNode) {
        val text = when (node.type) {
            PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT -> after(node, PlSqlKeyword.IMMEDIATE)
            PlSqlGrammar.OPEN_FOR_STATEMENT -> if (node.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) null else after(node, PlSqlKeyword.FOR)
            else -> parseStatement(node)
        } ?: return
        val value = followed(text, node) ?: return
        val whole = parameter(value)
        if (whole != null) {
            report(whole, valueOnly = false)
            return
        }
        val built = context.dynamicSql(value)
        if (built == null) {
            // A text that does not parse still runs every operand of the concatenation: without a tree, a value cannot
            // be told from a name.
            operands(value).forEach { operand -> parameter(operand)?.let { report(it, valueOnly = false) } }
            return
        }
        for (hole in built.holes) {
            val name = parameter(hole.expression) ?: continue
            report(name, writesOnlyValue(built, hole))
        }
    }

    /** The operands of the concatenation [value], through parentheses. */
    private fun operands(value: AstNode): List<AstNode> = when {
        value.type === PlSqlGrammar.CONCATENATION_EXPRESSION ->
            value.children.filter { it.type !== PlSqlGrammar.CONCATENATION_OPERATOR }.flatMap(::operands)
        value.type === PlSqlGrammar.BRACKED_EXPRESSION && value.numberOfChildren == 3 -> operands(value.children[1])
        else -> listOf(value)
    }

    private fun report(name: AstNode, valueOnly: Boolean) {
        if (!reported.add(name)) return
        addIssue(name, getLocalizedMessage(if (valueOnly) "valueMessage" else "message"), name.tokenOriginalValue)
    }

    /** Whether every token of the text that holds [hole]'s placeholder is a string literal: the hole writes only a value. */
    private fun writesOnlyValue(text: DynamicSqlText, hole: DynamicSqlHole): Boolean {
        if (text.holesDropped) return false
        val holders = text.root.descendants().filter { !it.hasChildren() && hole in text.holesIn(it) }.toList()
        return holders.isNotEmpty() && holders.all { it.token.type === PlSqlTokenType.STRING_LITERAL }
    }

    private fun AstNode.descendants(): Sequence<AstNode> = sequence {
        val pending = ArrayDeque<AstNode>().apply { add(this@descendants) }
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            yield(node)
            node.children.asReversed().forEach(pending::addLast)
        }
    }

    /** The statement text of a `DBMS_SQL.PARSE` call: its `statement` argument, else its second one. */
    private fun parseStatement(call: AstNode): AstNode? {
        val callee = call.firstChild.takeIf { it.type === PlSqlGrammar.MEMBER_EXPRESSION } ?: return null
        val name = callee.tokens.joinToString("") { it.value }.uppercase()
        if (name != "DBMS_SQL.PARSE" && name != "SYS.DBMS_SQL.PARSE") return null
        val arguments = call.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)?.getChildren(PlSqlGrammar.ARGUMENT).orEmpty()
        val named = arguments.firstOrNull { argument ->
            val tokens = argument.tokens
            tokens.size > 2 && tokens[0].value.equals("STATEMENT", ignoreCase = true) && tokens[1].value == "=>"
        }
        return (named ?: arguments.getOrNull(1))?.lastChild
    }

    private fun after(node: AstNode, keyword: PlSqlKeyword): AstNode? {
        val children = node.children
        val index = children.indexOfFirst { it.type === keyword }
        return if (index >= 0) children.getOrNull(index + 1) else null
    }

    /** [text] itself, or the one value a local variable written as [text] holds; null when the variable is not followed. */
    private fun followed(text: AstNode, statement: AstNode): AstNode? {
        val name = plainName(text) ?: return text
        val declaration = (context.nameBinding(name) as? NameBinding.Local)?.declaration ?: return text
        if (declaration.kind != NameDeclarationKind.VARIABLE) return text
        val node = declarationNode(name, declaration)?.takeIf { it.type === PlSqlGrammar.VARIABLE_DECLARATION } ?: return null
        val region = node.parentOrNull?.takeIf { it.type === PlSqlGrammar.DECLARE_SECTION }?.parentOrNull ?: return null
        if (region.type !in LOCAL_REGIONS || context.regionUncertainty(region)?.isEmpty() != true) return null
        return singleWrite(node, region, statement, text)
    }

    /**
     * The one value the variable declared by [declaration] receives in [region]: its default value or one `:=` of the
     * whole variable, which does not read the variable. Null for any other write (a second one, a part of the variable,
     * an `INTO`, an actual of a call that may write it), or an inner declaration of the same name.
     */
    private fun singleWrite(declaration: AstNode, region: AstNode, statement: AstNode, text: AstNode): AstNode? {
        val variable = declaration.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue
        if (declaredAgain(declaration, region, variable)) return null
        val values = ArrayList<AstNode>()
        declaration.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)?.let { values += it.lastChild }
        for (name in region.getDescendants(PlSqlGrammar.VARIABLE_NAME)) {
            if (name.tokenValue != variable) continue
            when (val write = writeAt(name, statement, text)) {
                is Write.Value -> values += write.value
                Write.Unseen -> return null
                null -> Unit
            }
        }
        val value = values.singleOrNull() ?: return null
        if ((listOf(value) + value.getDescendants(PlSqlGrammar.VARIABLE_NAME)).any { it.type === PlSqlGrammar.VARIABLE_NAME && it.tokenValue == variable }) return null
        return value
    }

    /** Whether a variable or parameter of [region] other than [declaration] has the name [variable]. */
    private fun declaredAgain(declaration: AstNode, region: AstNode, variable: String): Boolean =
        region.getDescendants(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.PARAMETER_DECLARATION).any {
            it !== declaration && it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenValue == variable
        }

    private sealed interface Write {
        /** The whole variable receives [value]. */
        class Value(val value: AstNode) : Write

        /** A write whose value is not in the code, or that writes a part of the variable. */
        data object Unseen : Write
    }

    /**
     * What the occurrence [name] of the variable does to it: a `:=` of the whole variable, a write the check does not
     * follow (a part of it, an `INTO`, an actual of a call other than [statement] that runs the [text]), or null when
     * it only reads it.
     */
    private fun writeAt(name: AstNode, statement: AstNode, text: AstNode): Write? {
        var target = name
        while (target.parentOrNull?.let { it.firstChild === target && it.type in COMPOSITES } == true) target = target.parent
        val parent = target.parent
        return when (parent.type) {
            PlSqlGrammar.ASSIGNMENT_STATEMENT -> {
                val operator = parent.children.indexOfFirst { it.type === PlSqlPunctuator.ASSIGNMENT }
                when {
                    parent.children.indexOf(target) > operator -> null
                    target !== name -> Write.Unseen
                    else -> Write.Value(parent.children[operator + 1])
                }
            }
            DmlGrammar.INTO_CLAUSE -> Write.Unseen
            PlSqlGrammar.ARGUMENT, PlSqlGrammar.UNNAMED_ACTUAL_PAMETER -> {
                val runsText = target === text || statement.type === PlSqlGrammar.METHOD_CALL && statement.isAncestorOf(target)
                Write.Unseen.takeIf { parent.lastChild === target && !runsText }
            }
            else -> null
        }
    }

    private fun AstNode.isAncestorOf(node: AstNode) = generateSequence(node.parentOrNull) { it.parentOrNull }.any { it === this }

    /** The plain name [node] is, through wrappers of one child. */
    private fun plainName(node: AstNode): AstNode? {
        var current = node
        while (current.numberOfChildren == 1 && current.type !== PlSqlGrammar.VARIABLE_NAME) current = current.firstChild
        return current.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME }
    }

    /**
     * The name of a character `IN` or `IN OUT` parameter that [expression] is directly, or passed to `DBMS_ASSERT.NOOP`;
     * null for anything else.
     */
    private fun parameter(expression: AstNode): AstNode? {
        val name = plainName(expression) ?: noopArgument(expression) ?: return null
        val declaration = (context.nameBinding(name) as? NameBinding.Local)?.declaration ?: return null
        if (declaration.kind != NameDeclarationKind.PARAMETER || declaration.fileId != null) return null
        val node = declarationNode(name, declaration)?.takeIf { it.type === PlSqlGrammar.PARAMETER_DECLARATION } ?: return null
        if (node.hasDirectChildren(PlSqlKeyword.OUT) && !node.hasDirectChildren(PlSqlKeyword.IN)) return null
        val category = context.valueCategory(name) as? ValueCategory.Scalar ?: return null
        return name.takeIf { category.family == ScalarFamily.CHARACTER || category.family == ScalarFamily.CLOB }
    }

    /** The argument of `DBMS_ASSERT.NOOP(x)`, which returns `x` unchanged. */
    private fun noopArgument(expression: AstNode): AstNode? {
        if (expression.type !== PlSqlGrammar.METHOD_CALL) return null
        val callee = expression.firstChild.tokens.joinToString("") { it.value }.uppercase()
        if (callee != "DBMS_ASSERT.NOOP" && callee != "SYS.DBMS_ASSERT.NOOP") return null
        val argument = expression.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)?.getChildren(PlSqlGrammar.ARGUMENT)?.singleOrNull() ?: return null
        return plainName(argument)
    }

    private fun declarationNode(from: AstNode, declaration: NameDeclaration): AstNode? {
        val byPosition = declarations ?: HashMap<Long, AstNode>().also { map ->
            val root = generateSequence(from) { it.parentOrNull }.last()
            for (node in root.getDescendants(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.PARAMETER_DECLARATION)) {
                val token = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.token ?: continue
                map.putIfAbsent(position(token.line, token.column), node)
            }
            declarations = map
        }
        return byPosition[position(declaration.line, declaration.column)]
    }

    private fun position(line: Int, column: Int) = line.toLong() shl 32 or column.toLong()

    private companion object {
        val LOCAL_REGIONS = setOf(
            PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.CREATE_FUNCTION
        )
        val COMPOSITES = setOf(PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.METHOD_CALL, PlSqlGrammar.OBJECT_REFERENCE)
    }
}
