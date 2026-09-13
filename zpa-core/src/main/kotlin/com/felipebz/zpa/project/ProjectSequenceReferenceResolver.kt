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
package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.squid.SemanticAstNode

internal enum class ProjectSequenceReferenceResolution {
    RESOLVED_SEQUENCE,
    RESOLVED_NON_SEQUENCE,
    UNKNOWN
}

/** Resolves only positive project sequence identity; all incomplete cases remain unknown. */
internal class ProjectSequenceReferenceResolver(
    private val context: ProjectAnalysisContext
) {

    fun resolve(node: AstNode): ProjectSequenceReferenceResolution {
        val candidate = sequenceCandidate(node) ?: return ProjectSequenceReferenceResolution.UNKNOWN
        if (hasAuthoritativeNonSequenceResolution(node)) {
            return ProjectSequenceReferenceResolution.RESOLVED_NON_SEQUENCE
        }
        if (hasLocalQualifierConflict(node, candidate.prefix)) {
            return ProjectSequenceReferenceResolution.UNKNOWN
        }

        val prepared = context.state as? ProjectAnalysisContext.State.Prepared
            ?: return ProjectSequenceReferenceResolution.UNKNOWN
        if (prepared.result.failures.isNotEmpty()) {
            return ProjectSequenceReferenceResolution.UNKNOWN
        }

        return when (prepared.result.index.findSequences(candidate.name).size) {
            1 -> ProjectSequenceReferenceResolution.RESOLVED_SEQUENCE
            else -> ProjectSequenceReferenceResolution.UNKNOWN
        }
    }

    private fun sequenceCandidate(node: AstNode): SequenceCandidate? {
        if (node.type != PlSqlGrammar.MEMBER_EXPRESSION) return null
        if (node.children.any { it.type == PlSqlPunctuator.REMOTE }) return null

        val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
        if (parts.size !in 2..3) return null
        val dots = node.children.count { it.type == PlSqlPunctuator.DOT }
        if (dots != parts.size - 1) return null

        val suffix = parts.last()
        if (suffix.type != PlSqlGrammar.IDENTIFIER_NAME ||
            suffix.getDescendants(PlSqlKeyword.NEXTVAL).isEmpty()) return null

        val prefix = parts.dropLast(1).mapNotNull(::identifier)
        if (prefix.size !in 1..2) return null
        return SequenceCandidate(QualifiedName(prefix), prefix)
    }

    private fun identifier(node: AstNode): OracleIdentifier? {
        val source = if (node.type == PlSqlGrammar.VARIABLE_NAME) {
            node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue.orEmpty()
        } else {
            node.tokenOriginalValue
        }
        return source.takeIf { it.isNotEmpty() }?.let(OracleIdentifier::fromSource)
    }

    private fun hasAuthoritativeNonSequenceResolution(node: AstNode): Boolean {
        val semantic = node as? SemanticAstNode ?: return false
        return semantic.symbol != null ||
            semantic.projectRecordMemberResolution != null ||
            semantic.projectRecordMemberPathResolution != null
    }

    private fun hasLocalQualifierConflict(node: AstNode, prefix: List<OracleIdentifier>): Boolean {
        val localQualifiers = sqlScopes(node).flatMap(::sqlScopeQualifiers)
        return localQualifiers.any { qualifier -> prefix.any { it == qualifier } }
    }

    private fun sqlScopes(node: AstNode): List<AstNode> {
        val result = mutableListOf<AstNode>()
        var ancestor = node.parentOrNull
        while (ancestor != null) {
            when (ancestor.type) {
                DmlGrammar.QUERY_BLOCK,
                DmlGrammar.SELECT_EXPRESSION,
                DmlGrammar.UPDATE_EXPRESSION,
                DmlGrammar.INSERT_EXPRESSION,
                DmlGrammar.MERGE_EXPRESSION -> result += ancestor
            }
            ancestor = ancestor.parentOrNull
        }
        return result
    }

    private fun sqlScopeQualifiers(scope: AstNode): List<OracleIdentifier> = when (scope.type) {
        DmlGrammar.QUERY_BLOCK -> queryBlockQualifiers(scope)
        DmlGrammar.SELECT_EXPRESSION -> selectExpressionQualifiers(scope)
        DmlGrammar.UPDATE_EXPRESSION,
        DmlGrammar.INSERT_EXPRESSION,
        DmlGrammar.MERGE_EXPRESSION -> dmlStatementQualifiers(scope)
        else -> emptyList()
    }

    private fun queryBlockQualifiers(queryBlock: AstNode): List<OracleIdentifier> {
        val aliases = queryBlock.getDescendants(DmlGrammar.ALIAS)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === queryBlock }
            .mapNotNull(::identifier)
        val tableNames = queryBlock.getDescendants(DmlGrammar.TABLE_REFERENCE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === queryBlock }
            .mapNotNull(::tableName)
        val commonTableExpressions = queryBlock.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === queryBlock }
            .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let(::identifier) }
        val valuesAliases = queryBlock.getDescendants(DmlGrammar.VALUES_EXPRESSION_CLAUSE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) === queryBlock }
            .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let(::identifier) }
        return aliases + tableNames + commonTableExpressions + valuesAliases
    }

    private fun selectExpressionQualifiers(selectExpression: AstNode): List<OracleIdentifier> {
        val queryBlocks = selectExpression.getChildren(DmlGrammar.QUERY_BLOCK)
        val commonTableExpressions = selectExpression.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.SELECT_EXPRESSION) === selectExpression }
            .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let(::identifier) }
        return queryBlocks.flatMap(::queryBlockQualifiers) + commonTableExpressions
    }

    private fun dmlStatementQualifiers(statement: AstNode): List<OracleIdentifier> {
        val aliases = statement.getDescendants(DmlGrammar.ALIAS)
            .filter { isDirectlyOwnedByStatement(it, statement) }
            .mapNotNull(::identifier)
        val tableNames = statement.getDescendants(DmlGrammar.TABLE_REFERENCE)
            .filter { isDirectlyOwnedByStatement(it, statement) }
            .mapNotNull(::tableName)
        val valuesAliases = statement.getDescendants(DmlGrammar.VALUES_EXPRESSION_CLAUSE)
            .filter { isDirectlyOwnedByStatement(it, statement) }
            .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let(::identifier) }
        val insertAliases = if (statement.type == DmlGrammar.INSERT_EXPRESSION) {
            statement.getDescendants(DmlGrammar.INSERT_INTO_CLAUSE)
                .filter { isDirectlyOwnedByStatement(it, statement) }
                .flatMap { it.getChildren(PlSqlGrammar.IDENTIFIER_NAME).mapNotNull(::identifier) }
        } else {
            emptyList()
        }
        val mergeTargetAliases = if (statement.type == DmlGrammar.MERGE_EXPRESSION) {
            statement.getChildren(PlSqlGrammar.IDENTIFIER_NAME).mapNotNull(::identifier)
        } else {
            emptyList()
        }
        return aliases + tableNames + valuesAliases + insertAliases + mergeTargetAliases
    }

    private fun isDirectlyOwnedByStatement(node: AstNode, statement: AstNode): Boolean =
        node.getFirstAncestorOrNull(DmlGrammar.QUERY_BLOCK) == null && when (statement.type) {
            DmlGrammar.UPDATE_EXPRESSION -> node.getFirstAncestorOrNull(DmlGrammar.UPDATE_EXPRESSION) === statement
            DmlGrammar.INSERT_EXPRESSION -> node.getFirstAncestorOrNull(DmlGrammar.INSERT_EXPRESSION) === statement
            DmlGrammar.MERGE_EXPRESSION -> node.getFirstAncestorOrNull(DmlGrammar.MERGE_EXPRESSION) === statement
            else -> false
        }

    private fun tableName(table: AstNode): OracleIdentifier? =
        table.getChildren(PlSqlGrammar.IDENTIFIER_NAME).lastOrNull()?.let(::identifier)

    private data class SequenceCandidate(
        val name: QualifiedName,
        val prefix: List<OracleIdentifier>
    )
}
