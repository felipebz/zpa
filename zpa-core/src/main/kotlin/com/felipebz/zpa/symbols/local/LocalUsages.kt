//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.UsageEvidence
import com.felipebz.zpa.api.semantics.UsageUnavailability
import com.felipebz.zpa.api.semantics.UsageUncertainty
import java.util.*

/**
 * The usage evidence of a file's declarations, from every reference's outcomes (refined with the project's
 * specification coverage when the project is prepared). Computed once per file.
 *
 * A reference whose outcome binds a declaration is a certain use, an erroneous one (a wrong kind, a duplicate, a
 * reference out of scope or written before the declaration) included: it names the declaration. A SQL name that may be a column, or an incomplete outcome whose provisional decision binds
 * the declaration, is a possible use. Absence is proven only when, besides, every region the declaration is visible in
 * (its own and those inside it) is complete and holds no unmodeled reference of the same name.
 */
internal class LocalUsages(private val facts: LocalSemanticFacts) {
    private val model = facts.model
    private val certain = HashSet<LocalDeclarationId>()
    private val possible = HashMap<LocalDeclarationId, EnumSet<UsageUncertainty>>()
    private val unsupported = HashSet<Pair<LocalRegionId, String>>()
    /** Member names of qualified references whose qualifier conditional compilation leaves undecided, by enclosing region. */
    private val conditionalMembers = HashSet<Pair<LocalRegionId, String>>()
    private val declarationsByName = model.declarations.associateBy { it.nameRange.startLine to it.nameRange.startColumn }
    private val incompleteness: Map<LocalRegionId, Subtree> by lazy(::subtreeIncompleteness)

    init {
        for ((index, reference) in facts.references.withIndex()) {
            val steps = facts.resolutions[index]
            for (step in steps) record(reference, step)
            recordUndecidedQualifier(reference, steps)
        }
    }

    /** The evidence for a declaration node of [COVERED] whose declaration the model holds. */
    fun evidence(declarationNode: AstNode): UsageEvidence {
        val declaration = declarationOf(declarationNode) ?: return UsageEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION)
        if (declaration.id in certain) return UsageEvidence.Used
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        possible[declaration.id]?.let(reasons::addAll)
        if (model.region(declaration.region).kind == LocalRegionKind.PACKAGE_SPEC) reasons += UsageUncertainty.VISIBLE_OUTSIDE_FILE
        // Conditional text that never writes the name, besides the declaration itself, holds no use of it.
        val own = LocalConditionalPiece.position(declaration.nameRange.startLine, declaration.nameRange.startColumn)
        incompleteness[declaration.region]?.let { subtree ->
            LocalRegionCompleteness.narrowed(subtree.reasons, subtree.pieces, declaration.name, own).forEach { reasons += it.usageUncertainty() }
        }
        if ((declaration.region to declaration.name.lookupName) in unsupported) reasons += UsageUncertainty.UNSUPPORTED_REFERENCE
        if ((declaration.region to declaration.name.lookupName) in conditionalMembers) reasons += UsageUncertainty.CONDITIONAL_COMPILATION
        return if (reasons.isEmpty()) UsageEvidence.Unused else UsageEvidence.Unknown(reasons)
    }

    private fun declarationOf(node: AstNode): LocalDeclaration? {
        val kinds = COVERED[node.type] ?: return null
        val name = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
        val declaration = declarationsByName[name.token.line to name.token.column] ?: return null
        return declaration.takeIf { it.kind in kinds }
    }

    private fun record(reference: LocalReference, outcome: LocalResolution) {
        when (outcome) {
            is LocalResolution.SpecificationCovered -> record(reference, outcome.outcome)
            is LocalResolution.Incomplete -> {
                val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
                outcome.regionReasons.forEach { reasons += it.usageUncertainty() }
                outcome.lookupReasons.forEach { reasons += uncertainty(it) }
                named(outcome.provisional).forEach { possible.getOrPut(it) { EnumSet.noneOf(UsageUncertainty::class.java) } += reasons }
            }
            is LocalResolution.SqlColumnOrLocal, is LocalResolution.SqlBuiltinOrLocal -> named(outcome).forEach {
                possible.getOrPut(it) { EnumSet.noneOf(UsageUncertainty::class.java) } += UsageUncertainty.SQL_NAME_MAY_BE_COLUMN
            }
            is LocalResolution.Unsupported -> {
                var region: LocalRegion? = model.region(reference.region)
                while (region != null) {
                    unsupported += region.id to reference.name.lookupName
                    region = model.parent(region.id)
                }
            }
            else -> certain += named(outcome)
        }
    }

    /**
     * A qualified [reference] whose qualifier (a step before its last segment) conditional compilation leaves
     * undecided: what the qualifier binds may differ, so each later segment may name any declaration of its name
     * visible from the reference (a label, unit, cursor or block name in the text may qualify another region).
     */
    private fun recordUndecidedQualifier(reference: LocalReference, steps: List<LocalResolution>) {
        val members = reference.segments.takeWhile { !it.isAttribute }
        val undecided = steps.take(members.size).indexOfFirst {
            it is LocalResolution.Incomplete && LocalIncompleteness.CONDITIONAL_COMPILATION in it.regionReasons
        }
        if (undecided < 0) return
        for (member in members.drop(undecided)) {
            var region: LocalRegion? = model.region(reference.region)
            while (region != null) {
                conditionalMembers += region.id to member.name.lookupName
                region = model.parent(region.id)
            }
        }
    }

    /** The declarations an outcome names. */
    private fun named(outcome: LocalResolution): List<LocalDeclarationId> = when (outcome) {
        is LocalResolution.Bound -> listOf(outcome.declaration)
        is LocalResolution.OverloadSet -> outcome.declarations
        is LocalResolution.Duplicate -> outcome.declarations
        is LocalResolution.WrongKind -> listOf(outcome.declaration)
        is LocalResolution.OutOfScope -> listOf(outcome.declaration)
        is LocalResolution.DeclaredLater -> listOf(outcome.declaration)
        is LocalResolution.SqlColumnOrLocal -> listOf(outcome.declaration)
        is LocalResolution.SqlBuiltinOrLocal -> outcome.declarations
        is LocalResolution.SpecificationCovered -> named(outcome.outcome)
        else -> emptyList()
    }

    /** The incompleteness of a region and every region inside it, with the conditional pieces behind it. */
    private class Subtree {
        val reasons: EnumSet<LocalIncompleteness> = EnumSet.noneOf(LocalIncompleteness::class.java)
        val pieces = ArrayList<LocalConditionalPiece>()
    }

    /** Each region with the incompleteness of every region inside it, itself included. */
    private fun subtreeIncompleteness(): Map<LocalRegionId, Subtree> {
        val result = HashMap<LocalRegionId, Subtree>()
        for (region in model.regions) {
            val reasons = region.completeness.reasons
            if (reasons.isEmpty()) continue
            val pieces = region.completeness.conditionalPieces()
            var current: LocalRegion? = region
            while (current != null) {
                val subtree = result.getOrPut(current.id, ::Subtree)
                subtree.reasons += reasons
                subtree.pieces += pieces
                current = model.parent(current.id)
            }
        }
        return result
    }


    private companion object {
        /** The declaration nodes covered, with the kinds of declaration each may hold. */
        val COVERED = mapOf(
            PlSqlGrammar.VARIABLE_DECLARATION to setOf(LocalDeclarationKind.VARIABLE),
            PlSqlGrammar.EXCEPTION_DECLARATION to setOf(LocalDeclarationKind.EXCEPTION),
            PlSqlGrammar.PARAMETER_DECLARATION to setOf(LocalDeclarationKind.PARAMETER),
            PlSqlGrammar.CURSOR_PARAMETER_DECLARATION to setOf(LocalDeclarationKind.CURSOR_PARAMETER),
            PlSqlGrammar.CURSOR_DECLARATION to setOf(LocalDeclarationKind.CURSOR)
        )
    }

    private fun uncertainty(reason: LocalLookupGap) = when (reason) {
        LocalLookupGap.SQL_COLUMN_UNKNOWN -> UsageUncertainty.SQL_NAME_MAY_BE_COLUMN
        LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE -> UsageUncertainty.DECLARATION_OUTSIDE_FILE
    }
}
