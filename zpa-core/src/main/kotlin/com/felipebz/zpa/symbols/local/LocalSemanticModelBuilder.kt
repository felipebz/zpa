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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.extraction.TokenStream
import com.felipebz.zpa.project.extraction.TypeReferences
import java.util.*

/** Source facts shared by the local extraction code: identifiers and ranges of AST nodes, never the nodes. */
internal object LocalSyntax {
    fun identifier(node: AstNode): OracleIdentifier = OracleIdentifier.fromSource(node.token.originalValue)

    fun range(fileId: FileId, node: AstNode): SourceRange =
        SourceRange(fileId, node.token.line, node.token.column, node.lastToken.endLine, node.lastToken.endColumn)
}

/**
 * Builds a [LocalSemanticModel] from an AST the parser already produced, in one traversal and without reparsing.
 * All construction state belongs to one call; the result keeps no AST node, token or source text.
 *
 * In the grammar, `BLOCK_STATEMENT` occurs only as the body of `ANONYMOUS_BLOCK` (a top-level block) and as a
 * `STATEMENT` alternative (a nested block); both form a region. Subprogram, trigger and type bodies have their own
 * productions and become [LocalRegionKind.UNMODELED] regions, as do the constructs that declare an index or iterand
 * outside a declarative section: FOR and FORALL loops and the iterator choices of qualified expressions. The member
 * wrappers of object types (`TYPE_SUBPROGRAM`, `MAP_ORDER_FUNCTION`) form no region: only the wrapped subprogram does.
 */
internal object LocalSemanticModelBuilder {
    private val UNMODELED_REGIONS = setOf(
        PlSqlGrammar.CREATE_PROCEDURE,
        PlSqlGrammar.CREATE_FUNCTION,
        PlSqlGrammar.CREATE_PACKAGE,
        PlSqlGrammar.CREATE_PACKAGE_BODY,
        PlSqlGrammar.CREATE_TRIGGER,
        PlSqlGrammar.COMPOUND_TRIGGER_BLOCK,
        PlSqlGrammar.TIMING_POINT_SECTION,
        PlSqlGrammar.CREATE_TYPE,
        PlSqlGrammar.CREATE_TYPE_BODY,
        PlSqlGrammar.TYPE_CONSTRUCTOR,
        PlSqlGrammar.PROCEDURE_DECLARATION,
        PlSqlGrammar.FUNCTION_DECLARATION,
        PlSqlGrammar.FOR_STATEMENT,
        PlSqlGrammar.FORALL_STATEMENT,
        PlSqlGrammar.BASIC_ITERATOR_CHOICE,
        PlSqlGrammar.INDEX_ITERATOR_CHOICE,
        PlSqlGrammar.SEQUENCE_ITERATOR_CHOICE
    )

    fun build(root: AstNode, fileId: FileId): LocalSemanticModel = Construction(fileId).build(root)

    private class RegionDraft(
        val id: LocalRegionId,
        val parent: RegionDraft?,
        val kind: LocalRegionKind,
        val syntax: String,
        val range: SourceRange
    ) {
        val declarations = ArrayList<LocalDeclarationId>()
        val children = ArrayList<LocalRegionId>()
        val gaps = ArrayList<LocalCoverageGap>()
    }

    private class Construction(private val fileId: FileId) {
        private val regions = ArrayList<RegionDraft>()
        private val declarations = ArrayList<LocalDeclaration>()
        private var recovered = false

        fun build(root: AstNode): LocalSemanticModel {
            val file = open(null, LocalRegionKind.FILE, "FILE", fileRange(root))
            walk(root, file)
            return freeze()
        }

        private fun open(parent: RegionDraft?, kind: LocalRegionKind, syntax: String, range: SourceRange): RegionDraft {
            val region = RegionDraft(LocalRegionId(regions.size), parent, kind, syntax, range)
            regions += region
            parent?.children?.add(region.id)
            return region
        }

        private fun walk(node: AstNode, region: RegionDraft) {
            for (child in node.children) visit(child, region)
        }

        private fun visit(node: AstNode, region: RegionDraft) {
            when {
                node.type === PlSqlGrammar.RECOVERY -> {
                    recovered = true
                    region.gaps += gap(LocalGapKind.PARSE_RECOVERY, node)
                }
                node.type === PlSqlGrammar.BLOCK_STATEMENT -> block(node, region)
                node.type in UNMODELED_REGIONS -> unmodeled(node, region)
                // A label of a statement that forms no region: a declaration of the region holding it.
                node.type === PlSqlGrammar.LABEL -> region.gaps += gap(LocalGapKind.LABEL, node)
                else -> walk(node, region)
            }
        }

        /** Opens the region formed by [node]; its labels are declarations of the enclosing [region]. */
        private fun openFormedBy(node: AstNode, region: RegionDraft, kind: LocalRegionKind): RegionDraft? {
            node.children.filter { it.type === PlSqlGrammar.LABEL }.forEach { region.gaps += gap(LocalGapKind.LABEL, it) }
            val first = node.children.firstOrNull { it.type !== PlSqlGrammar.LABEL } ?: return null
            val range = SourceRange(fileId, first.token.line, first.token.column, node.lastToken.endLine, node.lastToken.endColumn)
            return open(region, kind, node.type.toString(), range)
        }

        private fun block(node: AstNode, enclosing: RegionDraft) {
            val region = openFormedBy(node, enclosing, LocalRegionKind.ANONYMOUS_BLOCK) ?: return
            for (child in node.children) {
                when (child.type) {
                    PlSqlGrammar.LABEL -> Unit
                    PlSqlGrammar.DECLARE_SECTION -> child.children.forEach { declarationItem(it, region) }
                    else -> visit(child, region)
                }
            }
        }

        private fun unmodeled(node: AstNode, enclosing: RegionDraft) {
            val region = openFormedBy(node, enclosing, LocalRegionKind.UNMODELED) ?: return
            node.children.filter { it.type !== PlSqlGrammar.LABEL }.forEach { visit(it, region) }
        }

        private fun declarationItem(item: AstNode, region: RegionDraft) {
            val kind = when (item.type) {
                PlSqlGrammar.VARIABLE_DECLARATION -> LocalDeclarationKind.VARIABLE
                PlSqlGrammar.CUSTOM_SUBTYPE -> LocalDeclarationKind.SUBTYPE
                // Every PRAGMA_DECLARATION alternative is a pragma; none introduces a name (EXCEPTION_INIT names an
                // exception declared elsewhere), so it cannot hide a declaration and records no gap.
                PlSqlGrammar.PRAGMA_DECLARATION -> return
                else -> null
            }
            val datatype = item.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
            if (kind == null || datatype == null) {
                // Never silently dropped: the region records that it holds a declaration it did not extract. The item
                // is still visited, so that a region it forms (a local subprogram body) keeps its nested regions.
                region.gaps += gap(LocalGapKind.UNSUPPORTED_DECLARATION, item)
                visit(item, region)
                return
            }
            val name = item.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            val id = LocalDeclarationId(declarations.size)
            declarations += LocalDeclaration(
                id = id,
                region = region.id,
                position = region.declarations.size,
                kind = kind,
                name = LocalSyntax.identifier(name),
                range = LocalSyntax.range(fileId, item),
                nameRange = LocalSyntax.range(fileId, name),
                typeRange = LocalSyntax.range(fileId, datatype),
                typeRef = datatype.tokens.let { TypeReferences(TokenStream(fileId, it)).declaredType(0, it.size) },
                isConstant = item.hasDirectChildren(PlSqlKeyword.CONSTANT),
                isNotNull = item.getFirstChildOrNull(PlSqlGrammar.DATATYPE_NULL_CONSTRAINT)?.hasDirectChildren(PlSqlKeyword.NOT) == true
            )
            region.declarations += id
        }

        private fun gap(kind: LocalGapKind, node: AstNode) = LocalCoverageGap(kind, node.type.toString(), LocalSyntax.range(fileId, node))

        private fun fileRange(root: AstNode): SourceRange {
            val content = root.children.filter { it.type !== GenericTokenType.EOF && it.hasToken() }
            if (content.isEmpty()) return SourceRange(fileId, 1, 0, 1, 0)
            val first = content.first().token
            val last = content.last().lastToken
            return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
        }

        private fun freeze(): LocalSemanticModel {
            val frozen = regions.map { draft ->
                val extraction = when {
                    draft.kind == LocalRegionKind.UNMODELED -> LocalExtraction.NOT_EXTRACTED
                    draft.gaps.isNotEmpty() -> LocalExtraction.PARTIAL
                    else -> LocalExtraction.ALL_ITEMS_EXTRACTED
                }
                LocalRegion(
                    draft.id, draft.parent?.id, draft.kind, draft.syntax, draft.range,
                    frozenList(draft.declarations), frozenList(draft.children), extraction, frozenList(draft.gaps)
                )
            }
            val completeness = if (recovered) LocalCompleteness.PARSE_RECOVERY_OBSERVED else LocalCompleteness.NOT_ESTABLISHED
            return LocalSemanticModel(fileId, frozenList(frozen), frozenList(declarations), completeness)
        }

        private fun <T> frozenList(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
    }
}
