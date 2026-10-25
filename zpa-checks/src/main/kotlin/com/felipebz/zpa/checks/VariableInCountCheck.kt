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
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.matchers.MethodMatcher
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.api.semantics.NameDeclarationKind
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class VariableInCountCheck : AbstractBaseCheck() {

    private val count = MethodMatcher.create().name("count").addParameter()

    override fun init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL)
        subscribeTo(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        if (!node.parent.typeIs(DmlGrammar.SELECT_COLUMN)) {
            return
        }
        if (node.typeIs(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)) {
            if (node.firstChild.typeIs(PlSqlKeyword.COUNT)) {
                checkArguments(node, listOf(node.children.dropWhile { !it.typeIs(PlSqlKeyword.ALL) }[1]))
            }
            return
        }
        if (!count.matches(node)) {
            return
        }

        checkArguments(node, count.getArgumentsValues(node))
    }

    private companion object {
        val VALUES = setOf(NameDeclarationKind.VARIABLE, NameDeclarationKind.PARAMETER, NameDeclarationKind.LOOP_INDEX)
    }

    private fun checkArguments(currentNode: AstNode, arguments: List<AstNode>) {
        if (arguments.size != 1) {
            return
        }

        val value = arguments[0]
        val binding = context.nameBinding(value)
        if (binding is NameBinding.Local && binding.declaration.kind in VALUES) {
            addIssue(currentNode, getLocalizedMessage(), value.tokenOriginalValue)
        }
    }

}
