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
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.syntax.SelectStatement
import com.felipebz.zpa.api.syntax.SyntaxViews
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.CRITICAL)
@ConstantRemediation("20min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class QueryWithoutExceptionHandlingCheck : AbstractBaseCheck() {

    @RuleProperty(key = "strict", defaultValue = "true")
    var strictMode = true

    override fun init() {
        subscribeTo(SyntaxViews.SELECT_STATEMENT, ::visitSelectStatement)
    }

    private fun visitSelectStatement(statement: SelectStatement) {
        val intoClause = statement.queryBlocks.asSequence()
            .mapNotNull { it.intoClause }
            .firstOrNull()
        if (intoClause?.isBulkCollect == true) {
            return
        }

        if (isGuaranteedSingleRowAggregate(statement.astNode)) {
            return
        }

        if (strictMode) {
            val parentBlock = statement.astNode.getFirstAncestorOrNull(PlSqlGrammar.STATEMENTS_SECTION)

            if (parentBlock?.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLERS) == false) {
                addIssue(statement, getLocalizedMessage())
            }
        } else {
            val hasExceptionHandler = context.currentScope?.hasExceptionHandler ?: false
            if (!hasExceptionHandler) {
                addIssue(statement, getLocalizedMessage())
            }
        }
    }

    private fun isGuaranteedSingleRowAggregate(selectStatement: AstNode): Boolean {
        val intoClause = selectStatement.getFirstDescendantOrNull(DmlGrammar.INTO_CLAUSE) ?: return false
        if (intoClause.firstChild.typeIs(PlSqlKeyword.BULK)) return false

        val selectExpression = selectStatement.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION) ?: return false
        if (selectExpression.getChildren(DmlGrammar.QUERY_BLOCK).size != 1 ||
            selectExpression.hasDirectChildren(
                PlSqlKeyword.UNION,
                PlSqlKeyword.INTERSECT,
                PlSqlKeyword.EXCEPT,
                PlSqlKeyword.MINUS_KEYWORD,
                DmlGrammar.ROW_LIMITING_CLAUSE
            )
        ) {
            return false
        }

        val queryBlock = selectExpression.getFirstChildOrNull(DmlGrammar.QUERY_BLOCK) ?: return false
        if (queryBlock.hasDirectChildren(
                DmlGrammar.GROUP_BY_CLAUSE,
                DmlGrammar.HAVING_CLAUSE,
                DmlGrammar.MODEL_CLAUSE
            )
        ) {
            return false
        }

        return queryBlock.getChildren(DmlGrammar.SELECT_COLUMN).any { selectColumn ->
            selectColumn.getDescendants(
                PlSqlGrammar.METHOD_CALL,
                AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION
            ).any { aggregate ->
                aggregate.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === queryBlock &&
                    isAggregate(aggregate) &&
                    !isAnalytic(aggregate)
            }
        }
    }

    private fun isAggregate(node: AstNode): Boolean {
        if (node.type === AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION) return true
        if (node.type !== PlSqlGrammar.METHOD_CALL) return false

        val callee = node.children.firstOrNull {
            it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.MEMBER_EXPRESSION
        } ?: return false
        if (callee.type !== PlSqlGrammar.VARIABLE_NAME) return false

        val identifier = callee.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return false
        val spelling = identifier.token.originalValue
        return !spelling.startsWith("\"") && COMMON_AGGREGATE_FUNCTIONS.any {
            it.equals(spelling, ignoreCase = true)
        }
    }

    private fun isAnalytic(node: AstNode): Boolean {
        return node.getFirstAncestorOrNull(PlSqlGrammar.POSTFIX_EXPRESSION)
            ?.hasDescendant(DmlGrammar.ANALYTIC_CLAUSE) == true
    }

    private companion object {
        val COMMON_AGGREGATE_FUNCTIONS = setOf("count", "sum", "avg", "min", "max")
    }

}
