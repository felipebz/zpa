//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.tooling

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.semantics.HighlightingRange
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.symbols.local.LocalRegion
import com.felipebz.zpa.symbols.local.LocalSemanticFacts

/**
 * One node of the symbol tree: a region of the local resolution, a declaration with its kind and name, or a reference.
 * [sourceRanges] are what selecting the node shows: a region's extent, a declaration's name and its references, or the
 * reference itself.
 */
public class SymbolTreeNode(
    public val label: String,
    sourceRanges: List<SemanticSourceRange>,
    children: List<SymbolTreeNode> = emptyList()
) {
    public val sourceRanges: List<SemanticSourceRange> = sourceRanges.toList()
    public val children: List<SymbolTreeNode> = children.toList()

    override fun toString(): String = label
}

/** The regions of a file's local model, nested, each with its declarations and their references from the pairs. */
@OptIn(ZpaExperimentalApi::class)
internal class SymbolTreeBuilder(
    private val fileId: FileId,
    private val facts: LocalSemanticFacts,
    pairs: List<HighlightingPair>
) {
    private val model = facts.model
    private val references = pairs.associate { it.declaration to it.references }

    fun build(): SymbolTreeNode = region(model.root)

    private fun region(region: LocalRegion): SymbolTreeNode {
        val range = region.range
        val declarations = region.declarations.map(model::declaration).map { declaration ->
            val name = declaration.nameRange
            val uses = references[HighlightingRange(name.startLine, name.startColumn, name.endLine, name.endColumn)].orEmpty()
            val useNodes = uses.map { SymbolTreeNode("${it.startLine}:${it.startColumn}", listOf(range(it))) }
            SymbolTreeNode(
                "${declaration.kind} ${declaration.name.originalSpelling} (${name.startLine}:${name.startColumn})",
                listOf(range(name)) + uses.map(::range),
                useNodes.ifEmpty { listOf(SymbolTreeNode("<no references>", emptyList())) }
            )
        }
        val children = region.children.map { region(model.region(it)) }
        return SymbolTreeNode("${region.kind} (lines ${range.startLine}-${range.endLine})", listOf(range(range)), declarations + children)
    }

    private fun range(range: SourceRange) =
        SemanticSourceRange(fileId.value, range.startLine, range.startColumn, range.endLine, range.endColumn)

    private fun range(range: HighlightingRange) =
        SemanticSourceRange(fileId.value, range.startLine, range.startColumn, range.endLine, range.endColumn)
}
