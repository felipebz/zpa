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
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.SequenceReferenceResolution
import java.util.Collections
import java.util.IdentityHashMap

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class SequenceNextvalInConditionalResultCheck : AbstractBaseCheck() {

    private val reportedNextvals = Collections.newSetFromMap(IdentityHashMap<AstNode, Boolean>())

    override fun startScan() {
        super.startScan()
        reportedNextvals.clear()
    }

    override fun init() {
        subscribeTo(PlSqlGrammar.CASE_EXPRESSION)
        subscribeTo(PlSqlGrammar.METHOD_CALL)
    }

    override fun visitNode(node: AstNode) {
        when {
            node.type === PlSqlGrammar.CASE_EXPRESSION && isSqlExpression(node) -> checkCaseResults(node)
            node.type === PlSqlGrammar.METHOD_CALL && isBuiltInDecode(node) && isSqlExpression(node) -> {
                checkDecodeResults(node)
            }
        }
    }

    private fun isSqlExpression(node: AstNode): Boolean {
        return node.hasAncestor(
            PlSqlGrammar.SELECT_STATEMENT,
            PlSqlGrammar.INSERT_STATEMENT,
            PlSqlGrammar.UPDATE_STATEMENT,
            PlSqlGrammar.MERGE_STATEMENT
        )
    }

    private fun checkCaseResults(caseExpression: AstNode) {
        caseExpression.children.forEachIndexed { index, child ->
            if (child.`is`(PlSqlKeyword.THEN, PlSqlKeyword.ELSE)) {
                caseExpression.children.getOrNull(index + 1)?.let(::scanConditionalResult)
            }
        }
    }

    private fun checkDecodeResults(methodCall: AstNode) {
        val arguments = methodCall.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)
            ?.getChildren(PlSqlGrammar.ARGUMENT)
            .orEmpty()
        if (arguments.size < 3) return

        arguments.forEachIndexed { index, argument ->
            if (
                (index >= 2 && index % 2 == 0) ||
                (index == arguments.lastIndex && arguments.size % 2 == 0)
            ) {
                scanConditionalResult(argument.lastChild)
            }
        }
    }

    private fun isBuiltInDecode(methodCall: AstNode): Boolean {
        val callee = methodCall.children.firstOrNull {
            it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.MEMBER_EXPRESSION
        } ?: return false
        if (callee.type !== PlSqlGrammar.VARIABLE_NAME) return false

        val component = callee.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return false
        val spelling = component.token.originalValue
        return if (spelling.startsWith("\"") && spelling.endsWith("\"")) {
            spelling == "\"DECODE\""
        } else {
            spelling.equals("DECODE", ignoreCase = true)
        }
    }

    private fun scanConditionalResult(node: AstNode) {
        if (node.type === DmlGrammar.SELECT_EXPRESSION) {
            return
        }

        if (node.type === PlSqlKeyword.NEXTVAL) {
            if (isResolvedSequenceNextval(node) && reportedNextvals.add(node)) {
                addIssue(node, getLocalizedMessage())
            }
            return
        }

        node.children.forEach(::scanConditionalResult)
    }

    private fun isResolvedSequenceNextval(nextval: AstNode): Boolean {
        val memberExpression = nextval.getFirstAncestorOrNull(PlSqlGrammar.MEMBER_EXPRESSION) ?: return false
        return projectAnalysis().resolveSequenceReference(memberExpression) ==
            SequenceReferenceResolution.RESOLVED_SEQUENCE
    }
}
