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
        if (walk.completes(section.getFirstChild(PlSqlGrammar.STATEMENTS)) == Flow.CONTINUES) ends += section.getFirstChild(PlSqlKeyword.END)
        handlers(section).filter { walk.completes(it.getFirstChild(PlSqlGrammar.STATEMENTS)) == Flow.CONTINUES }
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
        private val procedures = HashMap<AstNode, Flow>()
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
         * cannot, the rest is unreachable, and execution does not reach the end of the list through it. A call this
         * does not see into may not return: as the last statement, it leaves the end of the list unknown; followed by
         * other statements, it is taken to return, since nobody writes code after a call that always raises.
         */
        fun completes(statements: AstNode): Flow {
            val list = statements.getChildren(PlSqlGrammar.STATEMENT)
            for ((index, statement) in list.withIndex()) {
                when (statement(statement.firstChild)) {
                    Flow.STOPS -> return Flow.STOPS
                    Flow.UNKNOWN -> if (index == list.lastIndex) return Flow.UNKNOWN
                    Flow.CONTINUES -> Unit
                }
            }
            return Flow.CONTINUES
        }

        private fun statement(node: AstNode): Flow = when (node.type) {
            PlSqlGrammar.RETURN_STATEMENT, PlSqlGrammar.RAISE_STATEMENT -> Flow.STOPS
            // Execution goes on after the loop, or to the next iteration: not to the next statement.
            PlSqlGrammar.EXIT_STATEMENT, PlSqlGrammar.CONTINUE_STATEMENT -> Flow.of(node.hasDirectChildren(PlSqlKeyword.WHEN))
            PlSqlGrammar.IF_STATEMENT ->
                if (node.hasDirectChildren(PlSqlGrammar.ELSE_CLAUSE)) any(branches(node)) else Flow.CONTINUES
            // Without ELSE, Oracle raises CASE_NOT_FOUND when no branch is taken.
            PlSqlGrammar.CASE_STATEMENT -> any(node.getChildren(PlSqlGrammar.STATEMENTS))
            PlSqlGrammar.LOOP_STATEMENT -> Flow.of(exits(node))
            PlSqlGrammar.WHILE_STATEMENT ->
                Flow.of(!alwaysTrue(node.children[node.children.indexOfFirst { it.type === PlSqlKeyword.WHILE } + 1]) || exits(node))
            PlSqlGrammar.BLOCK_STATEMENT -> section(node.getFirstChild(PlSqlGrammar.STATEMENTS_SECTION))
            PlSqlGrammar.CALL_STATEMENT -> call(node)
            else -> Flow.CONTINUES
        }

        /** The flow after one of several paths: on if any path goes on, else unknown if any path is unknown. */
        private fun any(paths: List<AstNode>): Flow = Flow.join(paths.map(::completes))

        private fun branches(node: AstNode): List<AstNode> =
            listOf(node.getFirstChild(PlSqlGrammar.STATEMENTS)) + node.getChildren(
                PlSqlGrammar.ELSIF_CLAUSE,
                PlSqlGrammar.ELSE_CLAUSE
            ).map { it.getFirstChild(PlSqlGrammar.STATEMENTS) }

        private fun section(section: AstNode): Flow =
            any(listOf(section.getFirstChild(PlSqlGrammar.STATEMENTS)) + handlers(section).map { it.getFirstChild(PlSqlGrammar.STATEMENTS) })

        private fun alwaysTrue(condition: AstNode): Boolean =
            condition.tokens.singleOrNull()?.value?.equals("TRUE", ignoreCase = true) == true

        /** Whether an EXIT leaves [loop]: one whose target, by its label or as the nearest loop around it, is [loop]. */
        private fun exits(loop: AstNode): Boolean =
            loop.getFirstChild(PlSqlGrammar.STATEMENTS).getDescendants(PlSqlGrammar.EXIT_STATEMENT)
                .any { target(it) === loop }

        private fun target(exit: AstNode): AstNode? {
            val label = exit.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenValue?.uppercase()
            var current = exit.parentOrNull
            while (current != null) {
                if (current.typeIs(LOOPS) && (label == null || labeled(current, label))) return current
                current = current.parentOrNull
            }
            return null
        }

        private fun labeled(loop: AstNode, label: String): Boolean =
            loop.getChildren(PlSqlGrammar.LABEL).any { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue.uppercase() == label }

        /**
         * A call that raises itself stops; one bound to a procedure of this file has the flow of that body. Any other
         * call is one this does not see into, and whether it returns is unknown.
         */
        private fun call(statement: AstNode): Flow {
            if (CheckUtils.isTerminationStatement(statement)) return Flow.STOPS
            val target = statement.children.firstOrNull { it.type !== PlSqlGrammar.LABEL } ?: return Flow.UNKNOWN
            if (target.type === PlSqlGrammar.METHOD_CALL && CheckUtils.isTerminationStatement(target)) return Flow.STOPS
            val name = if (target.type === PlSqlGrammar.METHOD_CALL) target.firstChild else target
            val binding = context.callBinding(name) as? CallBinding.Local ?: return Flow.UNKNOWN
            if (binding.status != LocalCallStatus.RESOLVED) return Flow.UNKNOWN
            val declaration = binding.selected ?: return Flow.UNKNOWN
            if (declaration.fileId != null) return Flow.UNKNOWN
            val procedure = declarations[declaration.line to declaration.column] ?: return Flow.UNKNOWN
            return procedure(procedure)
        }

        private fun procedure(procedure: AstNode): Flow = procedures[procedure] ?: run {
            val section = procedure.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)
            // A recursive call is taken to complete normally.
            if (section == null || !analyzable(procedure, section) || !visiting.add(procedure)) return Flow.CONTINUES
            val flow = section(section)
            visiting.remove(procedure)
            procedures[procedure] = flow
            flow
        }
    }

    /** Whether execution reaches the point after a statement: it does, it does not, or a call it depends on is unseen. */
    private enum class Flow {
        CONTINUES, STOPS, UNKNOWN;

        companion object {
            fun of(continues: Boolean) = if (continues) CONTINUES else STOPS

            fun join(flows: List<Flow>): Flow = when {
                CONTINUES in flows -> CONTINUES
                UNKNOWN in flows -> UNKNOWN
                else -> STOPS
            }
        }
    }

    private companion object {
        val LOOPS = arrayOf<AstNodeType>(PlSqlGrammar.LOOP_STATEMENT, PlSqlGrammar.WHILE_STATEMENT, PlSqlGrammar.FOR_STATEMENT)
    }
}
