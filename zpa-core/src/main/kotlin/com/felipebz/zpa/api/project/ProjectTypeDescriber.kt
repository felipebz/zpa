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
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.BuiltInTypeRefClassifier
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.PackageDeclaration
import com.felipebz.zpa.project.QualifiedName

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
        written: ProjectWrittenAnchor? = null
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
                    anchor(snapshot, name, written!!.anchor, null, AnchorContext.Written(startOwner?.name), steps, written)
                }
                reference?.kind == ProjectTypeReference.Kind.ANCHORED -> {
                    if (!holderKnown) holder = ReferenceHolders.find(snapshot.projectIndex, reference)
                    anchor(snapshot, reference.name, reference.anchor.get(), reference, AnchorContext.Retained(holder), steps, written)
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
            steps[declaration.id] = declaration
            reference = declaration.baseType
            name = declaration.baseType.name
            owner = declaration.owner
            holder = snapshot.occurrenceOf(declaration.id)?.let { ReferenceHolders.of(snapshot.projectIndex, it) }
            holderKnown = true
        }
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
        written: ProjectWrittenAnchor?
    ): Anchor {
        // The ordered copy is made only when a terminal description is built, never for a successful step.
        fun followed() = steps.values.toList()
        fun stop(why: ProjectTypeDescription.AnchorStop) =
            Anchor.Stop(ProjectTypeDescription.anchorStopped(followed(), reference, why, written))
        // %ROWTYPE stops before any owner or scope guard, so a written one-segment %ROWTYPE without owner stays ROWTYPE.
        if (kind == ProjectTypeReference.Anchor.ROWTYPE) return stop(ProjectTypeDescription.AnchorStop.ROWTYPE)
        val holder = (context as? AnchorContext.Retained)?.holder
        if (context is AnchorContext.Retained && holder == null) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        val index = snapshot.projectIndex
        val segments = anchorName.name.segments
        if (holder != null) {
            // A sibling record field or parameter captures the first segment before anything in the package.
            if (holder.capturingNames.any { it == segments.first() }) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
            // Body-private declarations are not facts, so a one-segment name in a body cannot be judged absent.
            if (holder.role == DeclarationRole.BODY && segments.size == 1) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        }
        val owner = when (context) {
            is AnchorContext.Retained -> holder!!.owner
            is AnchorContext.Written -> context.owner
        }
        if (owner == null) {
            // Standalone declarations have no package scope: only a qualified name can name a package value.
            if (segments.size == 1) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        } else {
            // A declaration of the holder's package named like the first segment captures it (positive scope evidence,
            // independent of the preparation state): a value of the package makes a qualified name a field path, and a
            // package declaration that is not a value makes the anchor invalid, so neither becomes a project lookup.
            val captured = index.findOccurrences(owner.append(segments.first()))
            val capturedValue = captured.any { it.declaration is com.felipebz.zpa.project.PackageValueDeclaration }
            if (segments.size >= 2 && captured.isNotEmpty()) {
                return stop(if (capturedValue) ProjectTypeDescription.AnchorStop.FIELD_PATH else ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
            }
            if (segments.size == 1 && captured.isNotEmpty() && !capturedValue) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        }
        // A package-body subprogram may be captured by a private declaration that is not a fact, whatever the qualification.
        if (owner != null && holder?.role == DeclarationRole.BODY)
            return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        // A shorter qualified package value followed by a field path competes with the fully qualified value.
        for (length in 2 until segments.size) {
            val prefix = QualifiedName(segments.subList(0, length))
            if (index.findOccurrences(prefix).any { it.declaration is com.felipebz.zpa.project.PackageValueDeclaration }) {
                return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH)
            }
        }
        val lookup = if (segments.size == 1) {
            snapshot.resolvePackageValue(anchorName, ProjectQualifiedName.wrap(owner!!))
        } else {
            snapshot.resolvePackageValue(anchorName, null)
        }
        if (lookup.status != ProjectValueResolution.Status.RESOLVED) {
            return Anchor.Stop(ProjectTypeDescription.unresolvedValue(followed(), lookup, reference, written))
        }
        val value = lookup.value.get()
        // The resolved value must be the only binding of its package: several specifications are not one scope.
        if (index.findOccurrences(value.owner.name).count { val d = it.declaration; d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION } != 1) {
            return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        }
        val valueOccurrence = snapshot.occurrenceOf(value.id) ?: return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        // Declaration order applies only within the specification that retains the reference; a written anchor is,
        // by precondition, not an indexed specification fact.
        if (holder != null && owner != null && holder.role != DeclarationRole.BODY) {
            val valueUnit = ReferenceHolders.unitOf(snapshot.projectIndex, valueOccurrence.declaration, value.owner.name)
            if (holder.unit == null) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
            if (valueUnit != null && valueUnit === holder.unit && !completedBefore(value.sourceRange, reference!!.sourceRange)) {
                return stop(ProjectTypeDescription.AnchorStop.DECLARED_LATER)
            }
        }
        if (steps.containsKey(value.id)) return Anchor.Cycle(value)
        return Anchor.Follow(value)
    }

    private fun completedBefore(value: ProjectSourceRange, reference: ProjectSourceRange): Boolean =
        value.endLine < reference.startLine || (value.endLine == reference.startLine && value.endColumn <= reference.startColumn)

    private fun category(name: ProjectQualifiedName): ProjectBuiltInTypeCategory? = when (BuiltInTypeRefClassifier.classify(name.name)) {
        PlSqlType.NUMERIC -> ProjectBuiltInTypeCategory.NUMERIC
        PlSqlType.CHARACTER -> ProjectBuiltInTypeCategory.CHARACTER
        PlSqlType.LOB -> ProjectBuiltInTypeCategory.LOB
        PlSqlType.BOOLEAN -> ProjectBuiltInTypeCategory.BOOLEAN
        PlSqlType.DATE -> ProjectBuiltInTypeCategory.DATE
        PlSqlType.JSON -> ProjectBuiltInTypeCategory.JSON
        else -> null
    }
}
