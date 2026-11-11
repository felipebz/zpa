//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HiddenDeclaration
import com.felipebz.zpa.api.semantics.HidingEvidence
import com.felipebz.zpa.api.semantics.UsageUnavailability
import com.felipebz.zpa.api.semantics.UsageUncertainty
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange
import java.util.*

/**
 * Which declaration a variable or exception declaration hides: the outermost variable, constant, exception, parameter,
 * cursor parameter or loop index of the same name visible where it is written, in the enclosing regions; another
 * declaration is visible when it ends before. A declaration of the declaration's own scope is a duplicate, not a hidden
 * one (Oracle rejects a reference with PLS-00371): its block, or for a package body declaration the body and its
 * specification, which form one scope. With a prepared project, the specification in another file of an enclosing
 * package body is searched too.
 */
internal class LocalHiding(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val declarationsByName = declarationStarts(model)

    fun evidence(declarationNode: AstNode): HidingEvidence {
        val declaration = declarationOf(declarationNode) ?: return HidingEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION)
        val search = search(declaration)
        search.specification?.let { hidden ->
            val own = reasons(model.region(declaration.region), declaration.name)
            return if (own.isEmpty()) HidingEvidence.Hides(hidden) else HidingEvidence.Unknown(own)
        }
        search.outermost?.let { hidden ->
            // Either declaration may not exist for Oracle when text it relies on is not visible to the analysis.
            val pair = reasons(model.region(declaration.region), declaration.name) + reasons(model.region(hidden.region), declaration.name)
            if (pair.isNotEmpty()) return HidingEvidence.Unknown(pair)
            return HidingEvidence.Hides(HiddenDeclaration(hidden.nameRange.startLine, hidden.nameRange.startColumn, null, node(declarationNode, hidden.nameRange)))
        }
        return if (search.reasons.isEmpty()) HidingEvidence.HidesNothing else HidingEvidence.Unknown(search.reasons)
    }

    private fun declarationOf(node: AstNode): LocalDeclaration? {
        if (node.type != PlSqlGrammar.VARIABLE_DECLARATION && node.type != PlSqlGrammar.EXCEPTION_DECLARATION) return null
        val name = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
        val index = declarationsByName[name.token.line, name.token.column]
        if (index < 0) return null
        return model.declarations[index].takeIf { it.kind == LocalDeclarationKind.VARIABLE || it.kind == LocalDeclarationKind.EXCEPTION }
    }

    /** What the enclosing regions declare of the name, and what the analysis could not see in them. */
    private class Search(val outermost: LocalDeclaration?, val specification: HiddenDeclaration?, val reasons: Set<UsageUncertainty>)

    private fun search(declaration: LocalDeclaration): Search {
        val unseen = EnumSet.noneOf(UsageUncertainty::class.java)
        var outermost: LocalDeclaration? = null
        var specification: HiddenDeclaration? = null
        // The declaration's own scope holds duplicates, not hidden declarations.
        var region: LocalRegion? = model.parent(declaration.region)
        while (region != null && region.kind != LocalRegionKind.FILE) {
            val scope = LocalResolver.scopeOf(model, region)
            scope.regions.forEach { unseen += reasons(it, declaration.name) }
            outermost = scope.named(declaration.name).lastOrNull { candidate ->
                candidate.kind in HIDDEN && (candidate.region in scope.earlierRegions || endsBefore(candidate.range, declaration.nameRange))
            } ?: outermost
            if (LocalLookupGap.SPECIFICATION_NOT_IN_FILE in scope.gaps) {
                when (val value = facts.specificationValues[region.id to declaration.name.lookupName]) {
                    is LocalSpecificationValue.Declared -> specification = HiddenDeclaration(value.line, value.column, value.fileId, null)
                    LocalSpecificationValue.None -> Unit
                    LocalSpecificationValue.Unknown, null -> unseen += UsageUncertainty.DECLARATION_OUTSIDE_FILE
                }
            }
            region = model.parent(region.id)
        }
        return Search(outermost, specification, unseen)
    }

    /** What the analysis cannot see in [region] that may declare, use or hide [name]. */
    private fun reasons(region: LocalRegion, name: OracleIdentifier): Set<UsageUncertainty> =
        region.completeness.reasonsFor(name).mapTo(EnumSet.noneOf(UsageUncertainty::class.java)) { it.usageUncertainty() }

    /** The name nodes of the file, by the start of their token: collected once, when a first hidden declaration is found. */
    private var namesByStart: LocalPositionMap<AstNode>? = null

    /** The declaration name node at [range] in the tree of [anyNode]. */
    private fun node(anyNode: AstNode, range: SourceRange): AstNode? {
        val names = namesByStart ?: run {
            var root = anyNode
            while (root.parentOrNull != null) root = root.parent
            // The first name node of each start, as a search in tree order finds it.
            val found = LocalPositionMap.Builder<AstNode>()
            root.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).forEach { found.put(it.token.line, it.token.column, it) }
            found.build(LocalPositionDuplicates.FIRST).also { namesByStart = it }
        }
        return names[range.startLine, range.startColumn]
    }

    private fun endsBefore(declaration: SourceRange, reference: SourceRange) =
        declaration.endLine < reference.startLine || declaration.endLine == reference.startLine && declaration.endColumn <= reference.startColumn

    private companion object {
        /** The kinds the check reads as hidden: variables (constants and exceptions included), parameters and loop indexes. */
        val HIDDEN = EnumSet.of(
            LocalDeclarationKind.VARIABLE, LocalDeclarationKind.EXCEPTION, LocalDeclarationKind.PARAMETER,
            LocalDeclarationKind.CURSOR_PARAMETER, LocalDeclarationKind.LOOP_INDEX
        )
    }
}
