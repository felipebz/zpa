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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectTypeDescriber.anchor
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.*

/** Why an anchor step stopped; recorded only for composition, never part of a public result. */
internal enum class AnchorStopCause {
    ROWTYPE, NO_HOLDER, SIBLING, BODY_SCOPE, NO_PACKAGE_SCOPE, PACKAGE_MEMBER, PREFIX_VALUE, PACKAGE_UNIT, DECLARED_LATER
}

/**
 * Optional, call-local record of the last anchor step: its holder, whether it was the written request, and why it
 * stopped with the declarations responsible. Filled by [ProjectTypeDescriber.describe]; it never changes the result.
 */
internal class AnchorTrace {
    var holder: ReferenceHolder? = null
    var written: Boolean = false
    var cause: AnchorStopCause? = null
    var captors: List<IndexedDeclaration> = emptyList()
}

/**
 * One finite, iterative walk over subtype aliases and supported source-backed `%TYPE` anchors. Named steps go through
 * [ProjectSnapshot.resolveNamedType] and value steps through [ProjectSnapshot.resolvePackageValue], so lookup rules,
 * candidate counting and failure precedence are exactly the existing ones; built-ins use the existing
 * [BuiltInTypeRefClassifier] before any project lookup. One visited set of declaration occurrences covers values and
 * subtypes. Nothing is cached or retained.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectTypeDescriber {
    private sealed interface Anchor {
        class Stop(val description: ProjectTypeDescription) : Anchor
        class Follow(val value: ProjectPackageValueView) : Anchor
        class Cycle(val value: ProjectPackageValueView) : Anchor
    }

    /**
     * Where the anchor of the current step is written: a retained reference with its (possibly unknown) holder, or a
     * caller-written request (first step only) whose lexical precedence the consumer has already established.
     */
    private sealed interface AnchorContext {
        class Retained(val holder: ReferenceHolder?) : AnchorContext
        class Written(val owner: QualifiedName?) : AnchorContext
    }

    @JvmSynthetic
    fun describe(
        snapshot: ProjectSnapshot,
        startName: ProjectQualifiedName,
        startReference: ProjectTypeReference?,
        startOwner: ProjectQualifiedName?,
        written: ProjectWrittenAnchor? = null,
        trace: AnchorTrace? = null
    ): ProjectTypeDescription {
        val steps = LinkedHashMap<ProjectDeclarationId, ProjectDeclarationView>()
        var name = startName
        var reference = startReference
        var owner = startOwner
        var holder: ReferenceHolder? = null
        var holderKnown = false
        var writtenStep = written != null
        while (true) {
            if (reference?.kind == ProjectTypeReference.Kind.REF) {
                return ProjectTypeDescription.refStopped(steps.values.toList(), reference, written)
            }
            val outcome = when {
                writtenStep -> {
                    writtenStep = false
                    anchor(snapshot, name, written!!.anchor, null, AnchorContext.Written(startOwner?.name), steps, written, trace)
                }
                reference?.kind == ProjectTypeReference.Kind.ANCHORED -> {
                    if (!holderKnown) holder = ReferenceHolders.find(snapshot.projectIndex, reference)
                    anchor(snapshot, reference.name, reference.anchor.get(), reference, AnchorContext.Retained(holder), steps, written, trace)
                }
                else -> null
            }
            when (outcome) {
                is Anchor.Stop -> return outcome.description
                is Anchor.Cycle -> return ProjectTypeDescription.cycle(steps.values.toList(), reference, written)
                is Anchor.Follow -> {
                    val value = outcome.value
                    steps[value.id] = value
                    reference = value.typeRef
                    name = value.typeRef.name
                    owner = value.owner
                    holder = snapshot.occurrenceOf(value.id)?.let { ReferenceHolders.of(snapshot.projectIndex, it) }
                    holderKnown = true
                    continue
                }
                null -> Unit
            }
            category(name)?.let { return ProjectTypeDescription.builtIn(steps.values.toList(), it, reference, written) }
            val resolution = snapshot.resolveNamedType(name, owner)
            terminalResolution(resolution, steps, reference, written)?.let { return it }
            val declaration = resolution.declaration.get() as ProjectSubtypeView
            steps[declaration.id] = declaration
            reference = declaration.baseType
            name = declaration.baseType.name
            owner = declaration.owner
            holder = snapshot.occurrenceOf(declaration.id)?.let { ReferenceHolders.of(snapshot.projectIndex, it) }
            holderKnown = true
        }
    }

    /** A resolution that ends the walk (unresolved, not a subtype, or an already visited subtype), or null to follow. */
    private fun terminalResolution(
        resolution: ProjectNamedTypeResolution,
        steps: Map<ProjectDeclarationId, ProjectDeclarationView>,
        reference: ProjectTypeReference?,
        written: ProjectWrittenAnchor?
    ): ProjectTypeDescription? {
        if (resolution.status != ProjectNamedTypeResolution.Status.RESOLVED) {
            return ProjectTypeDescription.unresolved(steps.values.toList(), resolution, reference, written)
        }
        val declaration = resolution.declaration.get()
        if (declaration !is ProjectSubtypeView) {
            return ProjectTypeDescription.declared(steps.values.toList(), declaration, reference, written)
        }
        if (steps.containsKey(declaration.id)) {
            return ProjectTypeDescription.cycle(steps.values.toList(), reference, written)
        }
        return null
    }

    /**
     * One `%TYPE`/`%ROWTYPE` step: a stop, or the one source value that the anchor names. [reference] is null only for
     * the written request, which has no retained syntax; the guards that need a retained holder (sibling capture,
     * body scope, declaration order) are the consumer's lexical gate for a written anchor and are skipped.
     */
    private fun anchor(
        snapshot: ProjectSnapshot,
        anchorName: ProjectQualifiedName,
        kind: ProjectTypeReference.Anchor,
        reference: ProjectTypeReference?,
        context: AnchorContext,
        steps: Map<ProjectDeclarationId, ProjectDeclarationView>,
        written: ProjectWrittenAnchor?,
        trace: AnchorTrace?
    ): Anchor {
        val step = AnchorStep(snapshot, reference, steps, written, trace)
        val holder = (context as? AnchorContext.Retained)?.holder
        trace?.holder = holder
        trace?.written = context is AnchorContext.Written
        trace?.cause = null
        trace?.captors = emptyList()
        // %ROWTYPE stops before any owner or scope guard, so a written one-segment %ROWTYPE without owner stays ROWTYPE.
        if (kind == ProjectTypeReference.Anchor.ROWTYPE) return step.stop(ProjectTypeDescription.AnchorStop.ROWTYPE, AnchorStopCause.ROWTYPE)
        if (context is AnchorContext.Retained && holder == null) return step.stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.NO_HOLDER)
        val index = snapshot.projectIndex
        val segments = anchorName.name.segments
        step.holderStop(holder, segments)?.let { return it }
        val owner = when (context) {
            is AnchorContext.Retained -> holder!!.owner
            is AnchorContext.Written -> context.owner
        }
        step.packageScopeStop(owner, holder, segments)?.let { return it }
        step.prefixValueStop(segments)?.let { return it }
        val lookup = if (segments.size == 1) {
            snapshot.resolvePackageValue(anchorName, ProjectQualifiedName.wrap(owner!!))
        } else {
            snapshot.resolvePackageValue(anchorName, null)
        }
        if (lookup.status != ProjectValueResolution.Status.RESOLVED) {
            return Anchor.Stop(ProjectTypeDescription.unresolvedValue(step.followed(), lookup, reference, written))
        }
        val value = lookup.value.get()
        // The resolved value must be the only binding of its package: several specifications are not one scope.
        if (index.findOccurrences(value.owner.name).count { val d = it.declaration; d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION } != 1) {
            return step.stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_UNIT)
        }
        val valueOccurrence = snapshot.occurrenceOf(value.id) ?: return step.stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_UNIT)
        step.declarationOrderStop(holder, owner, value, valueOccurrence)?.let { return it }
        if (steps.containsKey(value.id)) return Anchor.Cycle(value)
        return Anchor.Follow(value)
    }

    /** The stop checks of one [anchor] step, sharing the step's reference, walk and trace. */
    private class AnchorStep(
        private val snapshot: ProjectSnapshot,
        private val reference: ProjectTypeReference?,
        private val steps: Map<ProjectDeclarationId, ProjectDeclarationView>,
        private val written: ProjectWrittenAnchor?,
        private val trace: AnchorTrace?
    ) {
        // The ordered copy is made only when a terminal description is built, never for a successful step.
        fun followed() = steps.values.toList()

        fun stop(why: ProjectTypeDescription.AnchorStop, cause: AnchorStopCause, captors: List<IndexedDeclaration> = emptyList()): Anchor.Stop {
            trace?.cause = cause
            trace?.captors = captors
            return Anchor.Stop(ProjectTypeDescription.anchorStopped(followed(), reference, why, written))
        }

        fun holderStop(holder: ReferenceHolder?, segments: List<OracleIdentifier>): Anchor.Stop? {
            if (holder == null) return null
            // A sibling record field or parameter captures the first segment before anything in the package.
            if (holder.capturingNames.any { it == segments.first() }) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.SIBLING)
            // Body-private declarations are not facts, so a one-segment name in a body cannot be judged absent.
            if (holder.role == DeclarationRole.BODY && segments.size == 1) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.BODY_SCOPE)
            return null
        }

        fun packageScopeStop(owner: QualifiedName?, holder: ReferenceHolder?, segments: List<OracleIdentifier>): Anchor.Stop? {
            if (owner == null) {
                // Standalone declarations have no package scope: only a qualified name can name a package value.
                if (segments.size == 1) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.NO_PACKAGE_SCOPE)
            } else {
                packageMemberStop(owner, segments)?.let { return it }
            }
            // A package-body subprogram may be captured by a private declaration that is not a fact, whatever the qualification.
            if (owner != null && holder?.role == DeclarationRole.BODY)
                return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.BODY_SCOPE)
            return null
        }

        private fun packageMemberStop(owner: QualifiedName, segments: List<OracleIdentifier>): Anchor.Stop? {
            // A declaration of the holder's package named like the first segment captures it (positive scope evidence,
            // independent of the preparation state): a value of the package makes a qualified name a field path, and a
            // package declaration that is not a value makes the anchor invalid, so neither becomes a project lookup.
            val captured = snapshot.projectIndex.findOccurrences(owner.append(segments.first()))
            val capturedValue = captured.any { it.declaration is com.felipebz.zpa.project.PackageValueDeclaration }
            if (segments.size >= 2 && captured.isNotEmpty()) {
                return stop(
                    if (capturedValue) ProjectTypeDescription.AnchorStop.FIELD_PATH else ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE,
                    AnchorStopCause.PACKAGE_MEMBER, captured
                )
            }
            if (segments.size == 1 && captured.isNotEmpty() && !capturedValue) {
                return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_MEMBER, captured)
            }
            return null
        }

        fun prefixValueStop(segments: List<OracleIdentifier>): Anchor.Stop? {
            // A shorter qualified package value followed by a field path competes with the fully qualified value.
            for (length in 2 until segments.size) {
                val prefix = QualifiedName(segments.subList(0, length))
                val values = snapshot.projectIndex.findOccurrences(prefix).filter { it.declaration is com.felipebz.zpa.project.PackageValueDeclaration }
                if (values.isNotEmpty()) {
                    return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.PREFIX_VALUE, values)
                }
            }
            return null
        }

        fun declarationOrderStop(holder: ReferenceHolder?, owner: QualifiedName?, value: ProjectPackageValueView, valueOccurrence: IndexedDeclaration): Anchor.Stop? {
            // Declaration order applies only within the specification that retains the reference; a written anchor is,
            // by precondition, not an indexed specification fact.
            if (holder != null && owner != null && holder.role != DeclarationRole.BODY) {
                val valueUnit = ReferenceHolders.unitOf(snapshot.projectIndex, valueOccurrence.declaration, value.owner.name)
                if (holder.unit == null) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_UNIT)
                if (valueUnit != null && valueUnit === holder.unit && !completedBefore(value.sourceRange, reference!!.sourceRange)) {
                    return stop(ProjectTypeDescription.AnchorStop.DECLARED_LATER, AnchorStopCause.DECLARED_LATER, listOf(valueOccurrence))
                }
            }
            return null
        }
    }

    private fun completedBefore(value: ProjectSourceRange, reference: ProjectSourceRange): Boolean =
        value.endLine < reference.startLine || (value.endLine == reference.startLine && value.endColumn <= reference.startColumn)

    private fun category(name: ProjectQualifiedName): ProjectBuiltInTypeCategory? = builtInCategory(name.name)
}

/** The public category of a recognized built-in type name, using the existing classifier vocabulary. */
@OptIn(ZpaExperimentalApi::class)
internal fun builtInCategory(name: QualifiedName): ProjectBuiltInTypeCategory? = when (BuiltInTypeRefClassifier.classify(name)) {
    PlSqlType.NUMERIC -> ProjectBuiltInTypeCategory.NUMERIC
    PlSqlType.CHARACTER -> ProjectBuiltInTypeCategory.CHARACTER
    PlSqlType.LOB -> ProjectBuiltInTypeCategory.LOB
    PlSqlType.BOOLEAN -> ProjectBuiltInTypeCategory.BOOLEAN
    PlSqlType.DATE -> ProjectBuiltInTypeCategory.DATE
    PlSqlType.JSON -> ProjectBuiltInTypeCategory.JSON
    else -> null
}
