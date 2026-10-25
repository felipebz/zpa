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
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/**
 * The category of the type of a named value written in a file, from the file's reference outcomes and the type walk
 * over the scan's project. Computed per name when asked; it holds the project through [walk], so it lives on the visitor
 * context, never in the detached facts.
 */
internal class LocalValueCategories(private val facts: LocalSemanticFacts, private val walk: LocalTypeWalk) {
    private val model = facts.model
    private val referencesByStart = facts.references.indices
        .filter { facts.references[it].lastSegmentContext in CONTEXTS }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    fun category(name: AstNode): ValueCategory {
        val written = writtenSegments(name) ?: return unavailable()
        val index = referencesByStart[name.token.line to name.token.column] ?: return unavailable()
        val reference = facts.references[index]
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return unavailable()
        val steps = facts.resolutions[index]
        val first = steps.first()
        val target = QualifiedName(listOf(reference.name) + reference.segments.map { it.name })
        if (LocalReferenceOutcomes.isSpecificationMember(first)) {
            return walk.specificationMember(target.segments, reference.region)?.let { of(walk.projectValue(it, reference.region)) }
                ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        }
        if (LocalReferenceOutcomes.isAbsent(first)) {
            // One segment: a built-in, a function or a column; a qualified one is a schema object's value.
            return if (reference.segments.isEmpty()) unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            else of(walk.projectValue(target, reference.region))
        }
        val last = covered(steps.last())
        if (last is LocalResolution.Incomplete) return ValueCategory.Unknown(reasons(last))
        if (steps.size != reference.segments.size + 1) return unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        if (LocalReferenceOutcomes.isSpecificationMember(steps.last())) return of(walk.projectValue(target, reference.region))
        return when (last) {
            // In SQL a column of the same name would take it.
            is LocalResolution.SqlColumnOrLocal ->
                if (reference.sql?.excludesColumn(reference.name) == true) of(walk.value(last.declaration))
                else unknown(UsageUncertainty.SQL_NAME_MAY_BE_COLUMN)
            // `name(…)` in SQL: the local value, whose elements the call reads.
            is LocalResolution.SqlBuiltinOrLocal -> last.declarations.singleOrNull()?.let { of(walk.value(it)) }
                ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
            else -> of(walk.outcome(last))
        }
    }

    /** The number of segments after the first that [name] writes; null when it is not a value name of its own. */
    private fun writtenSegments(name: AstNode): Int? = when (name.type) {
        PlSqlGrammar.VARIABLE_NAME ->
            0.takeUnless { name.parentOrNull?.type === PlSqlGrammar.MEMBER_EXPRESSION && name.parentOrNull?.firstChild === name }
        PlSqlGrammar.MEMBER_EXPRESSION -> name.children.count { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME } - 1
        else -> null
    }

    private fun of(evidence: LocalTypeEvidence): ValueCategory {
        if (isRefCursorBuiltIn(evidence)) return ValueCategory.CursorVariable
        return terminal(evidence)
    }

    /**
     * A value written `SYS_REFCURSOR` that the walk handed out of the file: the predefined cursor type, which the project
     * does not classify. A local declaration of that name is followed locally and never reaches this.
     */
    private fun isRefCursorBuiltIn(evidence: LocalTypeEvidence): Boolean {
        if (evidence.terminal != LocalTypeEvidence.Terminal.PROJECT && evidence.terminal != LocalTypeEvidence.Terminal.BUILT_IN) return false
        val written = evidence.followed.lastOrNull()?.let(model::declaration)?.type?.typeRef as? NamedTypeRef ?: return false
        return written.name.segments.size == 1 && written.name.last.lookupName == "SYS_REFCURSOR"
    }

    private fun terminal(evidence: LocalTypeEvidence): ValueCategory = when (evidence.terminal) {
        LocalTypeEvidence.Terminal.COMPOSITE -> composite(evidence)
        LocalTypeEvidence.Terminal.BUILT_IN ->
            if (evidence.argumentType.family.isPresent) ValueCategory.Scalar else unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        LocalTypeEvidence.Terminal.PROJECT -> evidence.description?.let(::described) ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        LocalTypeEvidence.Terminal.LITERAL -> unavailable()
        LocalTypeEvidence.Terminal.UNKNOWN -> evidence.incomplete?.let { ValueCategory.Unknown(reasons(it)) }
            ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
    }

    /** A local record, collection or ref cursor type, a cursor's row, a query loop's record, or `SELF`. */
    private fun composite(evidence: LocalTypeEvidence): ValueCategory {
        val last = evidence.followed.lastOrNull()?.let(model::declaration) ?: return ValueCategory.ObjectInstance
        return when (last.kind) {
            LocalDeclarationKind.RECORD_TYPE -> ValueCategory.Record(RecordOrigin.RECORD_TYPE)
            LocalDeclarationKind.CURSOR, LocalDeclarationKind.LOOP_INDEX -> ValueCategory.Record(RecordOrigin.ROWTYPE)
            LocalDeclarationKind.REF_CURSOR_TYPE -> ValueCategory.CursorVariable
            LocalDeclarationKind.COLLECTION_TYPE -> {
                val kind = when ((last.details as? LocalCollectionType)?.form) {
                    LocalCollectionForm.ASSOCIATIVE_ARRAY -> CollectionKind.ASSOCIATIVE_ARRAY
                    LocalCollectionForm.NESTED_TABLE -> CollectionKind.NESTED_TABLE
                    LocalCollectionForm.VARRAY -> CollectionKind.VARRAY
                    null -> null
                }
                ValueCategory.Collection(kind, of(walk.written(last.type)))
            }
            else -> unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        }
    }

    /** A type the project describes; only a conclusive description classifies. */
    private fun described(description: ProjectExternalTypeDescription): ValueCategory {
        if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) return ValueCategory.Record(RecordOrigin.ROWTYPE)
        val source = description.source
        return when (source.status) {
            ProjectTypeDescription.Status.DECLARED_TYPE -> shaped(source.declaration.orElse(null) as? ProjectShapedTypeView)
            ProjectTypeDescription.Status.BUILT_IN -> ValueCategory.Scalar
            ProjectTypeDescription.Status.ANCHORED_REFERENCE ->
                if (source.anchorStop.orElse(null) == ProjectTypeDescription.AnchorStop.ROWTYPE) ValueCategory.Record(RecordOrigin.ROWTYPE)
                else unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            else -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        }
    }

    private fun shaped(type: ProjectShapedTypeView?): ValueCategory = when (type?.shape?.orElse(null)) {
        ProjectTypeShape.RECORD -> ValueCategory.Record(RecordOrigin.RECORD_TYPE)
        ProjectTypeShape.OBJECT -> ValueCategory.ObjectInstance
        ProjectTypeShape.REF_CURSOR -> ValueCategory.CursorVariable
        ProjectTypeShape.COLLECTION -> {
            // The project keeps the element's written reference only: a %ROWTYPE element is a row, anything else unknown.
            val element = type.elementType.orElse(null)
            ValueCategory.Collection(
                null,
                if (element?.anchor?.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) ValueCategory.Record(RecordOrigin.ROWTYPE)
                else unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            )
        }
        null -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
    }

    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach {
            reasons += when (it) {
                LocalIncompleteness.CONDITIONAL_COMPILATION -> UsageUncertainty.CONDITIONAL_COMPILATION
                LocalIncompleteness.SUBSTITUTION -> UsageUncertainty.SUBSTITUTION
                LocalIncompleteness.PARSE_RECOVERY -> UsageUncertainty.PARSE_RECOVERY
                else -> UsageUncertainty.INCOMPLETE_SOURCE
            }
        }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private fun unknown(reason: UsageUncertainty) = ValueCategory.Unknown(EnumSet.of(reason))

    private fun unavailable() = ValueCategory.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)

    private companion object {
        /** A name read as a value: in PL/SQL, in SQL, in `VALUES`, or called (an element access or a call). */
        val CONTEXTS = EnumSet.of(
            LocalReferenceContext.VALUE, LocalReferenceContext.SQL_VALUE, LocalReferenceContext.INSERT_VALUE,
            LocalReferenceContext.CALL_EXPRESSION, LocalReferenceContext.SQL_CALL
        )
    }
}
