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

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.symbols.local.LocalReferenceContext as Ctx

/** One written actual with its datatype evidence. */
internal class LocalActualEvidence(val actual: LocalActual, val evidence: LocalTypeEvidence) {
    override fun toString() = "$actual: $evidence"
}

internal enum class LocalCallOutcome {
    /** The first segment is proven absent: the project resolved and narrowed the call. */
    PROJECT,

    /** The first segment is declared locally (a callee, or a capture of the qualifier): the project is not consulted. */
    LOCAL_CAPTURE,

    /** The first segment's outcome is incomplete: a region or a lookup gap may hide a local declaration. */
    INCOMPLETE,

    /** The first segment's outcome, or the call's form, is not modeled. */
    UNSUPPORTED
}

/**
 * A call from local code: its written target, the first segment's outcome, the evidence of each actual, and the
 * project's typed resolution when the call was forwarded.
 */
internal class LocalCallAnalysis(
    val reference: LocalReference,
    val target: QualifiedName,
    val kind: ProjectCallKind,
    val firstSegment: LocalResolution,
    val outcome: LocalCallOutcome,
    /** Empty when the call form is not modeled. */
    val actuals: List<LocalActualEvidence>,
    /** Present only for [LocalCallOutcome.PROJECT]. */
    val typed: ProjectTypedCallResolution?
) {
    override fun toString() = "$kind $target: $outcome ($firstSegment) $actuals"
}

/**
 * Forwards calls whose callee is not local to the Project System: [ProjectSnapshot.resolveCall] on the written target,
 * then [ProjectSnapshot.narrowCall] with one piece of local evidence per written actual. Only a first segment proven
 * absent, with no gap and no incomplete region, reaches the project; any other outcome keeps the call local, and the
 * outcome says why.
 */
internal class LocalForwardCalls(
    private val model: LocalSemanticModel,
    private val references: List<LocalReference>,
    calls: List<LocalCall>,
    private val snapshot: ProjectSnapshot,
    private val metadata: ProjectExternalMetadata = ProjectExternalMetadata.none()
) {
    private val walk = LocalTypeWalk(model, snapshot, metadata)
    private val callsByPosition = calls.associateBy { it.line to it.column }
    private val namesByPosition = references.filter { it.context != Ctx.TYPE && it.context != Ctx.ANCHOR_TYPE && it.context != Ctx.ANCHOR_ROWTYPE }
        .groupBy { it.range.startLine to it.range.startColumn }

    /** Every call of the file, in reference order. */
    fun analyze(): List<LocalCallAnalysis> = references.mapNotNull { reference -> kindOf(reference)?.let { analyze(reference, it) } }

    private fun kindOf(reference: LocalReference): ProjectCallKind? {
        if (reference.segments.any { it.isAttribute }) return null
        return when (reference.lastSegmentContext) {
            Ctx.CALL_STATEMENT -> ProjectCallKind.PROCEDURE
            Ctx.CALL_EXPRESSION -> ProjectCallKind.FUNCTION
            else -> null
        }
    }

    private fun analyze(reference: LocalReference, kind: ProjectCallKind): LocalCallAnalysis {
        val target = QualifiedName(listOf(reference.name) + reference.segments.map { it.name })
        val first = LocalResolver.resolve(model, reference)
        val call = callsByPosition[reference.range.startLine to reference.range.startColumn]
        val actuals = call?.actuals?.map { LocalActualEvidence(it, evidence(it)) }.orEmpty()
        val outcome = when (first) {
            LocalResolution.ProvenAbsent -> if (call == null || call.actuals != null) LocalCallOutcome.PROJECT else LocalCallOutcome.UNSUPPORTED
            is LocalResolution.Incomplete -> LocalCallOutcome.INCOMPLETE
            is LocalResolution.Unsupported -> LocalCallOutcome.UNSUPPORTED
            else -> LocalCallOutcome.LOCAL_CAPTURE
        }
        if (outcome != LocalCallOutcome.PROJECT) return LocalCallAnalysis(reference, target, kind, first, outcome, actuals, null)
        val notation = ProjectCallArguments.of(actuals.map { actual ->
            actual.actual.label?.let { ProjectCallArgument.named(ProjectIdentifier.fromSource(it.originalSpelling)) } ?: ProjectCallArgument.positional()
        })
        val structural = snapshot.resolveCall(ProjectCallTarget.written(ProjectQualifiedName.wrap(target)), kind, notation)
        val typed = snapshot.narrowCall(structural, actuals.map { it.evidence.argumentType }, metadata, null)
        return LocalCallAnalysis(reference, target, kind, first, outcome, actuals, typed)
    }

    /** Evidence of one actual: a literal as Oracle types it, a name through the resolver and the type walk. */
    private fun evidence(actual: LocalActual): LocalTypeEvidence {
        if (!actual.isName) return LocalTypeEvidence.literal(actual.literal).takeIf { actual.literal != null }
            ?: LocalTypeEvidence.unknown("expression not modeled")
        val reference = namesByPosition[actual.nameLine to actual.nameColumn]?.singleOrNull()
            ?: return LocalTypeEvidence.unknown("name not collected")
        if (reference.segments.any { it.isAttribute }) return LocalTypeEvidence.unknown("attribute")
        val steps = LocalResolver.resolveQualified(model, reference)
        val first = steps.first()
        if (first == LocalResolution.ProvenAbsent) {
            // A one-segment name could be a parameterless function or a built-in: only a qualified one names a value.
            return if (reference.segments.isEmpty()) LocalTypeEvidence.unknown("not a local name")
            else walk.projectValue(QualifiedName(listOf(reference.name) + reference.segments.map { it.name }), reference.region)
        }
        if (steps.size != reference.segments.size + 1) return LocalTypeEvidence.unknown("stopped at ${steps.last()}")
        return walk.outcome(steps.last())
    }
}
