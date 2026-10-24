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
    private val declarationsByName = model.declarations.associateBy { it.nameRange.startLine to it.nameRange.startColumn }
    private val incompleteness: Map<LocalRegionId, Set<LocalIncompleteness>> by lazy(::subtreeIncompleteness)

    init {
        for ((index, reference) in facts.references.withIndex()) {
            for (step in facts.resolutions[index]) record(reference, step)
        }
    }

    /** The evidence for a declaration node of [COVERED] whose declaration the model holds. */
    fun evidence(declarationNode: AstNode): UsageEvidence {
        val declaration = declarationOf(declarationNode) ?: return UsageEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION)
        if (declaration.id in certain) return UsageEvidence.Used
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        possible[declaration.id]?.let(reasons::addAll)
        if (model.region(declaration.region).kind == LocalRegionKind.PACKAGE_SPEC) reasons += UsageUncertainty.VISIBLE_OUTSIDE_FILE
        incompleteness[declaration.region]?.forEach { reasons += uncertainty(it) }
        if ((declaration.region to declaration.name.lookupName) in unsupported) reasons += UsageUncertainty.UNSUPPORTED_REFERENCE
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
                outcome.regionReasons.forEach { reasons += uncertainty(it) }
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

    /** Each region with the incompleteness of every region inside it, itself included. */
    private fun subtreeIncompleteness(): Map<LocalRegionId, Set<LocalIncompleteness>> {
        val result = HashMap<LocalRegionId, EnumSet<LocalIncompleteness>>()
        for (region in model.regions) {
            val reasons = region.completeness.reasons
            if (reasons.isEmpty()) continue
            var current: LocalRegion? = region
            while (current != null) {
                result.getOrPut(current.id) { EnumSet.noneOf(LocalIncompleteness::class.java) } += reasons
                current = model.parent(current.id)
            }
        }
        return result
    }

    private fun uncertainty(reason: LocalIncompleteness) = when (reason) {
        LocalIncompleteness.CONDITIONAL_COMPILATION -> UsageUncertainty.CONDITIONAL_COMPILATION
        LocalIncompleteness.SUBSTITUTION -> UsageUncertainty.SUBSTITUTION
        LocalIncompleteness.PARSE_RECOVERY -> UsageUncertainty.PARSE_RECOVERY
        LocalIncompleteness.PARTIAL_EXTRACTION, LocalIncompleteness.DISCARDED_SOURCE_TEXT,
        LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS, LocalIncompleteness.SOURCE_TEXT_UNAVAILABLE -> UsageUncertainty.INCOMPLETE_SOURCE
    }

    private companion object {
        /** The declaration nodes covered, with the kinds of declaration each may hold. */
        val COVERED = mapOf(
            PlSqlGrammar.VARIABLE_DECLARATION to setOf(LocalDeclarationKind.VARIABLE),
            PlSqlGrammar.EXCEPTION_DECLARATION to setOf(LocalDeclarationKind.EXCEPTION),
            PlSqlGrammar.PARAMETER_DECLARATION to setOf(LocalDeclarationKind.PARAMETER),
            PlSqlGrammar.CURSOR_PARAMETER_DECLARATION to setOf(LocalDeclarationKind.CURSOR_PARAMETER)
        )
    }

    private fun uncertainty(reason: LocalLookupGap) = when (reason) {
        LocalLookupGap.SQL_COLUMN_UNKNOWN -> UsageUncertainty.SQL_NAME_MAY_BE_COLUMN
        LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE -> UsageUncertainty.DECLARATION_OUTSIDE_FILE
    }
}
