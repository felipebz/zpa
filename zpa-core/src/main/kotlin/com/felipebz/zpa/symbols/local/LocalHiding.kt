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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HiddenDeclaration
import com.felipebz.zpa.api.semantics.HidingEvidence
import com.felipebz.zpa.api.semantics.UsageUnavailability
import com.felipebz.zpa.api.semantics.UsageUncertainty
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
    private val declarationsByName = model.declarations.associateBy { it.nameRange.startLine to it.nameRange.startColumn }

    fun evidence(declarationNode: AstNode): HidingEvidence {
        val declaration = declarationOf(declarationNode) ?: return HidingEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION)
        val search = search(declaration)
        search.specification?.let { hidden ->
            val own = reasons(model.region(declaration.region))
            return if (own.isEmpty()) HidingEvidence.Hides(hidden) else HidingEvidence.Unknown(own)
        }
        search.outermost?.let { hidden ->
            // Either declaration may not exist for Oracle when text it relies on is not visible to the analysis.
            val pair = reasons(model.region(declaration.region)) + reasons(model.region(hidden.region))
            if (pair.isNotEmpty()) return HidingEvidence.Unknown(pair)
            return HidingEvidence.Hides(HiddenDeclaration(hidden.nameRange.startLine, hidden.nameRange.startColumn, null, node(declarationNode, hidden.nameRange)))
        }
        return if (search.reasons.isEmpty()) HidingEvidence.HidesNothing else HidingEvidence.Unknown(search.reasons)
    }

    private fun declarationOf(node: AstNode): LocalDeclaration? {
        if (node.type != PlSqlGrammar.VARIABLE_DECLARATION && node.type != PlSqlGrammar.EXCEPTION_DECLARATION) return null
        val name = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
        return declarationsByName[name.token.line to name.token.column]
            ?.takeIf { it.kind == LocalDeclarationKind.VARIABLE || it.kind == LocalDeclarationKind.EXCEPTION }
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
            scope.regions.forEach { unseen += reasons(it) }
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

    private fun reasons(region: LocalRegion): Set<UsageUncertainty> = region.completeness.reasons.mapTo(EnumSet.noneOf(UsageUncertainty::class.java)) { it.usageUncertainty() }

    /** The declaration name node at [range] in the tree of [anyNode]. */
    private fun node(anyNode: AstNode, range: SourceRange): AstNode? {
        var root = anyNode
        while (root.parentOrNull != null) root = root.parent
        return root.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).firstOrNull { it.token.line == range.startLine && it.token.column == range.startColumn }
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
