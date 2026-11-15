//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.ColumnBinding
import com.felipebz.zpa.api.semantics.ColumnSource
import com.felipebz.zpa.api.semantics.ColumnSourceKind
import com.felipebz.zpa.project.SourceRange

internal class LocalSqlSourceSymbols(
    private val facts: LocalSemanticFacts,
    private val root: AstNode,
    private val binding: (AstNode) -> ColumnBinding,
    private val source: (AstNode) -> ColumnSource?
) {
    class Symbol(val declaration: SourceRange, val references: List<SourceRange>)

    private val fileId = facts.model.fileId

    fun symbols(): List<Symbol> {
        val declarations = HashMap<ColumnSource, AstNode?>()
        val references = LinkedHashMap<AstNode, MutableList<SourceRange>>()
        withNames(references)
        for (name in root.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)) {
            if (name.type === PlSqlGrammar.VARIABLE_NAME && name.parent.type === PlSqlGrammar.MEMBER_EXPRESSION) continue
            if (!leftToSql(name) && !isCorrelationArgument(name) && !isWrittenColumn(name)) continue
            val segments = segments(name) ?: continue
            val source = when (val answer = binding(name)) {
                is ColumnBinding.Column -> answer.source
                is ColumnBinding.QueryColumn -> answer.source
                is ColumnBinding.Source -> answer.source
                else -> continue
            }
            val declaration = declarations.getOrPut(source) { declaration(source) } ?: continue
            val qualifier = qualifier(segments, source, LocalSyntax.identifier(declaration).lookupName) ?: continue
            references.getOrPut(declaration) { ArrayList() } += LocalSyntax.range(fileId, qualifier)
        }
        return references.map { (declaration, ranges) -> Symbol(LocalSyntax.range(fileId, declaration), ranges) }
    }

    private fun withNames(references: MutableMap<AstNode, MutableList<SourceRange>>) {
        val definitions = root.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
        if (definitions.isEmpty()) return
        definitions.forEach { references[it.firstChild] = ArrayList() }
        val names = definitions.mapTo(HashSet()) { LocalSyntax.identifier(it.firstChild).lookupName }
        for (reference in root.getDescendants(DmlGrammar.TABLE_REFERENCE)) {
            val name = reference.children.singleOrNull()?.takeIf { it.type === PlSqlGrammar.IDENTIFIER_NAME } ?: continue
            if (LocalSyntax.identifier(name).lookupName !in names) continue
            val definition = source(reference)?.withDefinition ?: continue
            references[definition.firstChild]?.add(LocalSyntax.range(fileId, name))
        }
    }

    /**
     * The segment of a name that names [source], whose declared name is [declared]: the first segment, or the relation
     * segment of `schema.relation.column` for a relation without alias. Segments after it are columns, attributes or
     * methods.
     */
    private fun qualifier(segments: List<AstNode>, source: ColumnSource, declared: String): AstNode? {
        fun names(segment: AstNode) = LocalSyntax.identifier(segment).lookupName == declared
        if (names(segments.first())) return segments.first()
        return segments.getOrNull(1)?.takeIf { source.alias == null && names(it) }
    }

    private fun leftToSql(name: AstNode): Boolean {
        val outcome = facts.steps(name.token.line, name.token.column)?.firstOrNull() ?: return false
        return ((outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome) == LocalResolution.SqlRelation
    }

    /**
     * A qualified column SQL writes, which no local reference holds: the left side of `SET` (one column or a list) and
     * a column of an `INSERT` column list.
     */
    private fun isWrittenColumn(name: AstNode): Boolean {
        if (name.type !== PlSqlGrammar.MEMBER_EXPRESSION || facts.steps(name.token.line, name.token.column) != null) return false
        return name.getFirstAncestorOrNull(DmlGrammar.UPDATE_COLUMN, DmlGrammar.INSERT_COLUMNS) != null
    }

    private fun isCorrelationArgument(name: AstNode): Boolean {
        if (name.type !== PlSqlGrammar.VARIABLE_NAME || name.parent.type !== PlSqlGrammar.ARGUMENT) return false
        val call = name.parent.parentOrNull?.parentOrNull?.takeIf { it.type === PlSqlGrammar.METHOD_CALL } ?: return false
        val callee = call.firstChild.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return false
        return LocalSyntax.identifier(callee).lookupName in CORRELATION_FUNCTIONS
    }

    private fun segments(name: AstNode): List<AstNode>? {
        if (name.type === PlSqlGrammar.VARIABLE_NAME) return listOf(name)
        val parts = name.children.filter { it.type !== PlSqlPunctuator.DOT }
        return parts.takeIf { it.all { part -> part.type === PlSqlGrammar.VARIABLE_NAME || part.type === PlSqlGrammar.IDENTIFIER_NAME } }
    }

    private fun declaration(source: ColumnSource): AstNode? {
        val alias = source.alias
        if (alias != null) return aliasNode(source.node)?.takeIf { LocalSyntax.identifier(it).lookupName == alias }
        if (source.kind == ColumnSourceKind.WITH_QUERY) return source.withDefinition?.firstChild
        if (source.kind != ColumnSourceKind.RELATION || source.node.type !== DmlGrammar.TABLE_REFERENCE) return null
        return source.node.children.takeWhile { it.type !== PlSqlPunctuator.REMOTE }.lastOrNull { it.type === PlSqlGrammar.IDENTIFIER_NAME }
    }

    private fun aliasNode(node: AstNode): AstNode? {
        val clause = if (node.type === DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE) node else node.parent
        clause.getFirstChildOrNull(DmlGrammar.ALIAS)?.let { return it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) }
        return if (node.type === DmlGrammar.TABLE_REFERENCE) node.parent.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) else null
    }

    private companion object {
        val CORRELATION_FUNCTIONS = setOf("REF", "VALUE")
    }
}
