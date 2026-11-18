//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.CallBinding
import com.felipebz.zpa.api.semantics.LocalCallStatus
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG], category = RuleCategory.INCOMPLETE)
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class FunctionWithoutReturnCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CREATE_FUNCTION, PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.TYPE_CONSTRUCTOR)
    }

    /** The analysis of the current file: its local procedures and the answer for each, kept until the next file. */
    private var walk = Walk()

    override fun visitFile(node: AstNode) {
        walk = Walk()
    }

    override fun visitNode(node: AstNode) {
        val section = node.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION) ?: return
        // A pipelined function ends with a bare RETURN, or none: Oracle ends the rows it piped.
        if (headerTokens(node).any { it.type === PlSqlKeyword.PIPELINED }) return
        if (!analyzable(node, section)) return
        val ends = ArrayList<AstNode>()
        if (walk.completes(section.getFirstChild(PlSqlGrammar.STATEMENTS))) ends += section.getFirstChild(PlSqlKeyword.END)
        handlers(section).filter { walk.completes(it.getFirstChild(PlSqlGrammar.STATEMENTS)) }
            .forEach { ends += it.getFirstChild(PlSqlKeyword.WHEN) }
        if (ends.isEmpty()) return
        val issue = addIssue(nameNode(node), getLocalizedMessage())
        ends.forEach { issue.secondary(it, getLocalizedMessage("fallThroughMessage")) }
    }

    /** No GOTO, whose target this does not follow, and no conditional-compilation selection the settings do not decide. */
    private fun analyzable(unit: AstNode, section: AstNode): Boolean =
        !section.hasDescendant(PlSqlGrammar.GOTO_STATEMENT) && unit.tokens.none { token -> token.trivia.any { it.token.type === PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY } }

    private fun headerTokens(unit: AstNode) =
        unit.children.takeWhile { it.type !== PlSqlGrammar.STATEMENTS_SECTION && it.type !== PlSqlGrammar.DECLARE_SECTION }
            .flatMap { it.tokens }

    private fun nameNode(unit: AstNode): AstNode {
        val unitName = unit.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
        if (unitName != null) return unitName.getLastChild(PlSqlGrammar.IDENTIFIER_NAME)
        return unit.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
    }

    private fun handlers(section: AstNode): List<AstNode> =
        section.getFirstChildOrNull(PlSqlGrammar.EXCEPTION_HANDLERS)?.getChildren(PlSqlGrammar.EXCEPTION_HANDLER)
            .orEmpty()

    /** The "can complete normally" analysis of one file, with the local procedures its functions asked about. */
    private inner class Walk {
        private val procedures = HashMap<AstNode, Boolean>()
        private val visiting = HashSet<AstNode>()
        private val declarations: Map<Pair<Int, Int>, AstNode> by lazy(LazyThreadSafetyMode.NONE) {
            val root = context.rootTree() ?: return@lazy emptyMap()
            root.getDescendants(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.CREATE_PROCEDURE)
                .associateBy { procedure ->
                    val name = procedure.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
                        ?.getLastChild(PlSqlGrammar.IDENTIFIER_NAME)
                        ?: procedure.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
                    name.token.line to name.token.column
                }
        }

        /**
         * Whether execution can go on after the statement list [statements]: every statement can. After one that
         * cannot, the rest is unreachable, and execution does not reach the end of the list through it.
         */
        fun completes(statements: AstNode): Boolean =
            statements.getChildren(PlSqlGrammar.STATEMENT).all { statement(it.firstChild) }

        private fun statement(node: AstNode): Boolean = when (node.type) {
            PlSqlGrammar.RETURN_STATEMENT, PlSqlGrammar.RAISE_STATEMENT -> false
            // Execution goes on after the loop, or to the next iteration: not to the next statement.
            PlSqlGrammar.EXIT_STATEMENT, PlSqlGrammar.CONTINUE_STATEMENT -> node.hasDirectChildren(PlSqlKeyword.WHEN)
            PlSqlGrammar.IF_STATEMENT -> !node.hasDirectChildren(PlSqlGrammar.ELSE_CLAUSE) || branches(node).any(::completes)
            // Without ELSE, Oracle raises CASE_NOT_FOUND when no branch is taken.
            PlSqlGrammar.CASE_STATEMENT -> node.getChildren(PlSqlGrammar.STATEMENTS).any(::completes)
            PlSqlGrammar.LOOP_STATEMENT -> exits(node)
            PlSqlGrammar.WHILE_STATEMENT -> !alwaysTrue(node.children[node.children.indexOfFirst { it.type === PlSqlKeyword.WHILE } + 1]) || exits(
                node
            )

            PlSqlGrammar.BLOCK_STATEMENT -> section(node.getFirstChild(PlSqlGrammar.STATEMENTS_SECTION))
            PlSqlGrammar.CALL_STATEMENT -> call(node)
            else -> true
        }

        private fun branches(node: AstNode): List<AstNode> =
            listOf(node.getFirstChild(PlSqlGrammar.STATEMENTS)) + node.getChildren(
                PlSqlGrammar.ELSIF_CLAUSE,
                PlSqlGrammar.ELSE_CLAUSE
            ).map { it.getFirstChild(PlSqlGrammar.STATEMENTS) }

        private fun section(section: AstNode): Boolean =
            completes(section.getFirstChild(PlSqlGrammar.STATEMENTS)) || handlers(section).any {
                completes(
                    it.getFirstChild(
                        PlSqlGrammar.STATEMENTS
                    )
                )
            }

        private fun alwaysTrue(condition: AstNode): Boolean =
            condition.tokens.singleOrNull()?.value?.equals("TRUE", ignoreCase = true) == true

        /** Whether an EXIT leaves [loop]: one whose target, by its label or as the nearest loop around it, is [loop]. */
        private fun exits(loop: AstNode): Boolean =
            loop.getFirstChild(PlSqlGrammar.STATEMENTS).getDescendants(PlSqlGrammar.EXIT_STATEMENT)
                .any { target(it) === loop }

        private fun target(exit: AstNode): AstNode? {
            val label = exit.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenValue?.uppercase()
            var current = exit.parent
            while (current != null) {
                if (current.typeIs(LOOPS) && (label == null || labeled(current, label))) return current
                current = current.parent
            }
            return null
        }

        private fun labeled(loop: AstNode, label: String): Boolean =
            loop.getChildren(PlSqlGrammar.LABEL).any { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue.uppercase() == label }

        /** A call completes normally unless it raises itself, or binds a procedure of this file whose body cannot complete. */
        private fun call(statement: AstNode): Boolean {
            if (CheckUtils.isTerminationStatement(statement)) return false
            val target = statement.children.firstOrNull { it.type !== PlSqlGrammar.LABEL } ?: return true
            if (target.type === PlSqlGrammar.METHOD_CALL && CheckUtils.isTerminationStatement(target)) return false
            val name = if (target.type === PlSqlGrammar.METHOD_CALL) target.firstChild else target
            val binding = context.callBinding(name) as? CallBinding.Local ?: return true
            if (binding.status != LocalCallStatus.RESOLVED) return true
            val declaration = binding.selected ?: return true
            if (declaration.fileId != null) return true
            val procedure = declarations[declaration.line to declaration.column] ?: return true
            return procedure(procedure)
        }

        private fun procedure(procedure: AstNode): Boolean = procedures[procedure] ?: run {
            val section = procedure.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)
            // A recursive call is taken to complete normally.
            if (section == null || !analyzable(procedure, section) || !visiting.add(procedure)) return true
            val completes = section(section)
            visiting.remove(procedure)
            procedures[procedure] = completes
            completes
        }
    }

    private companion object {
        val LOOPS = arrayOf<AstNodeType>(PlSqlGrammar.LOOP_STATEMENT, PlSqlGrammar.WHILE_STATEMENT, PlSqlGrammar.FOR_STATEMENT)
    }
}
