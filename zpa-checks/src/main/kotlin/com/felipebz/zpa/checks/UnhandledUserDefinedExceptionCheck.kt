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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.ExceptionBinding
import com.felipebz.zpa.api.semantics.ExceptionDeclaration
import com.felipebz.zpa.api.semantics.ExceptionErrorCode
import com.felipebz.zpa.api.semantics.ExceptionScope

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@OptIn(ZpaExperimentalApi::class)
class UnhandledUserDefinedExceptionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val identifier = raisedException(node) ?: return
        val exception = (context.exceptionBinding(identifier) as? ExceptionBinding.Declared)?.exception ?: return

        // Another subprogram of the package may handle it: not enough information.
        if (exception.scope == ExceptionScope.PACKAGE_SPECIFICATION || exception.scope == ExceptionScope.PACKAGE_BODY) return
        // Tied to an error code (or maybe): raised as that error, which any handler for the code, in any unit, catches.
        if (exception.errorCode != ExceptionErrorCode.None) return
        if (isHandled(node, exception) == false) {
            addIssue(node, getLocalizedMessage(), identifier.tokens.joinToString("") { it.value })
        }
    }

    private fun raisedException(node: AstNode): AstNode? {
        return node.getFirstChildOrNull(PlSqlKeyword.RAISE)?.nextSibling
            ?.takeUnless { it.type === PlSqlPunctuator.SEMICOLON }
    }

    private fun isHandled(raise: AstNode, exception: ExceptionDeclaration): Boolean? {
        var block = enclosingBlock(raise)
        while (block != null) {
            if (context.regionUncertainty(block)?.isEmpty() != true)
                return null

            if (!raisedOutsideBody(raise, block)) {
                when (handles(block, exception)) {
                    true -> return true
                    null -> return null
                    false -> Unit
                }
            }
            if (block.type != PlSqlGrammar.BLOCK_STATEMENT) {
                // A caller may handle an exception it can name: only one declared in this unit is out of its reach.
                return false.takeIf { declaredIn(exception, block) }
            }
            block = enclosingBlock(block)
        }
        return false
    }

    private fun raisedOutsideBody(raise: AstNode, block: AstNode): Boolean {
        val handlers = block.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)?.getFirstChildOrNull(PlSqlGrammar.EXCEPTION_HANDLERS)
        val declarations = block.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)
        return (handlers != null && raise.isInside(handlers)) || (declarations != null && raise.isInside(declarations))
    }

    private fun handles(block: AstNode, exception: ExceptionDeclaration): Boolean? {
        // A unit whose handlers are elsewhere (the timing points of a compound trigger) is not followed.
        val statements = block.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION) ?: return null
        val handlers = statements.getFirstChildOrNull(PlSqlGrammar.EXCEPTION_HANDLERS) ?: return false
        var undecided = false
        for (handler in handlers.getChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
            if (handler.hasDirectChildren(PlSqlKeyword.OTHERS) && !handler.hasDescendant(PlSqlKeyword.SQLERRM)) {
                return true
            }
            for (name in handler.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)) {
                when (val binding = context.exceptionBinding(name)) {
                    is ExceptionBinding.Declared -> if (binding.exception == exception) return true
                    is ExceptionBinding.Unknown, is ExceptionBinding.Unavailable -> undecided = true
                    else -> Unit
                }
            }
        }
        return false.takeUnless { undecided }
    }

    private fun declaredIn(exception: ExceptionDeclaration, unit: AstNode): Boolean {
        val line = exception.line ?: return false
        val column = exception.column ?: return false
        val first = unit.token
        val last = unit.lastToken ?: return false
        return (line > first.line || line == first.line && column >= first.column) &&
            (line < last.line || line == last.line && column <= last.column)
    }

    private fun enclosingBlock(node: AstNode): AstNode? = node.getFirstAncestorOrNull(*BLOCKS)

    private fun AstNode.isInside(ancestor: AstNode): Boolean {
        var current: AstNode? = parentOrNull
        while (current != null) {
            if (current === ancestor) return true
            current = current.parentOrNull
        }
        return false
    }

    companion object {
        /** The nodes with handlers: blocks, and the subprograms and triggers that bound propagation. */
        private val BLOCKS = arrayOf<AstNodeType>(
            PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION, PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.TYPE_CONSTRUCTOR,
            PlSqlGrammar.SIMPLE_DML_TRIGGER, PlSqlGrammar.INSTEAD_OF_DML_TRIGGER, PlSqlGrammar.SYSTEM_TRIGGER,
            PlSqlGrammar.CREATE_TRIGGER, PlSqlGrammar.CREATE_PACKAGE_BODY
        )
    }

}
