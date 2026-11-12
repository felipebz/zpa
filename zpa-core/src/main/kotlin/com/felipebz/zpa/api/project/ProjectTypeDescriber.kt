//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*

/** Why an anchor step stopped; recorded only for composition, never part of a public result. */
internal enum class AnchorStopCause {
    REMOTE, ROWTYPE, NO_HOLDER, SIBLING, BODY_SCOPE, BODY_MEMBER, NO_PACKAGE_SCOPE, PACKAGE_MEMBER, PREFIX_VALUE, PACKAGE_UNIT, DECLARED_LATER,
    FIELD_NOT_FOUND, FIELD_UNDECIDED
}

/** A value the walk followed, with the anchor that named it (its reference is null for the written request). */
@OptIn(ZpaExperimentalApi::class)
internal class FollowedAnchor(
    val name: ProjectQualifiedName,
    val reference: ProjectTypeReference?,
    val value: ProjectPackageValueView,
    /** True when the first segment was bound in the package scope of the anchor (a one-segment name or a field path). */
    val packageScope: Boolean,
    val selectsFields: Boolean
)

/**
 * Optional, call-local record of the last anchor step: its holder, whether it was the written request, and why it
 * stopped with the declarations responsible; and every value followed, in walk order. For a field the walk could not
 * select, the step is the anchor whose field path names it. Filled by [ProjectTypeDescriber.describe]; it never
 * changes the result.
 */
internal class AnchorTrace {
    var holder: ReferenceHolder? = null
    var written: Boolean = false
    var cause: AnchorStopCause? = null
    var captors: List<IndexedDeclaration> = emptyList()
    val followed = ArrayList<FollowedAnchor>()
}

/**
 * One finite, iterative walk over subtype aliases and supported source-backed `%TYPE` anchors. Named steps go through
 * [ProjectSnapshot.resolveNamedType] and value steps through [ProjectSnapshot.resolvePackageValue], so lookup rules,
 * candidate counting and failure precedence are exactly the existing ones; built-ins use the existing
 * [BuiltInTypeRefClassifier] before any project lookup. An anchor that names fields after a package value
 * (`p.g.f%TYPE`, `g.f%TYPE` in the package of `g`) follows the value, then selects each field from the record or
 * object type the walk reaches ([ProjectSnapshot.memberResolution]) and continues with the field's written type. One
 * visited set of declaration occurrences covers values, subtypes and the types fields are selected from. Nothing is
 * cached or retained.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectTypeDescriber {
    private sealed interface Anchor {
        class Stop(val description: ProjectTypeDescription) : Anchor
        class Follow(val value: ProjectPackageValueView, val fields: List<OracleIdentifier>, val packageScope: Boolean) : Anchor
        class Cycle(val value: ProjectPackageValueView) : Anchor
    }

    /**
     * The fields still to select, outermost first. Each anchor that names a field path pushes its fields in front of
     * those of the anchors that led to it, so the first field always belongs to the most recently followed path.
     */
    private class PendingFields {
        private class Field(val name: OracleIdentifier, val origin: Int)

        private class Origin(val reference: ProjectTypeReference?, val holder: ReferenceHolder?)

        private val fields = ArrayDeque<Field>()
        private val origins = ArrayList<Origin>()

        fun isEmpty() = fields.isEmpty()

        fun push(names: List<OracleIdentifier>, reference: ProjectTypeReference?, holder: ReferenceHolder?) {
            if (names.isEmpty()) return
            val origin = origins.size
            origins += Origin(reference, holder)
            for (i in names.indices.reversed()) fields.addFirst(Field(names[i], origin))
        }

        fun first(): OracleIdentifier = fields.first().name

        fun select() {
            fields.removeFirst()
        }

        fun path(): List<ProjectIdentifier> = if (fields.isEmpty()) emptyList() else fields.map { ProjectIdentifier.wrap(it.name) }

        /**
         * A stop at the first field: the description stops at the anchor whose field path names it, with the fields of
         * the enclosing anchors as its path, and the trace records that anchor.
         */
        fun stop(
            steps: Map<ProjectDeclarationId, ProjectDeclarationView>,
            why: ProjectTypeDescription.AnchorStop,
            cause: AnchorStopCause,
            written: ProjectWrittenAnchor?,
            trace: AnchorTrace?
        ): ProjectTypeDescription {
            val origin = origins[fields.first().origin]
            val own = fields.first().origin
            while (fields.isNotEmpty() && fields.first().origin == own) fields.removeFirst()
            trace?.holder = origin.holder
            trace?.written = origin.reference == null
            trace?.cause = cause
            trace?.captors = emptyList()
            return ProjectTypeDescription.anchorStopped(steps.values.toList(), origin.reference, why, written, path())
        }
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
        val pending = PendingFields()
        var name = startName
        var reference = startReference
        var owner = startOwner
        var holder: ReferenceHolder? = null
        var holderKnown = false
        var writtenStep = written != null
        while (true) {
            if (reference?.kind == ProjectTypeReference.Kind.REF) {
                return ProjectTypeDescription.refStopped(steps.values.toList(), reference, written, pending.path())
            }
            val anchorReference = if (writtenStep) null else reference
            val outcome = when {
                writtenStep -> {
                    writtenStep = false
                    AnchorStep(snapshot, null, steps, written, trace, pending).anchor(name, written!!.anchor, AnchorContext.Written(startOwner?.name))
                }
                reference?.kind == ProjectTypeReference.Kind.ANCHORED -> {
                    if (!holderKnown) holder = ReferenceHolders.find(snapshot.projectIndex, reference)
                    AnchorStep(snapshot, reference, steps, written, trace, pending).anchor(reference.name, reference.anchor.get(), AnchorContext.Retained(holder))
                }
                else -> null
            }
            when (outcome) {
                is Anchor.Stop -> return outcome.description
                is Anchor.Cycle -> return ProjectTypeDescription.cycle(steps.values.toList(), reference, written, pending.path())
                is Anchor.Follow -> {
                    val value = outcome.value
                    trace?.followed?.add(FollowedAnchor(name, anchorReference, value, outcome.packageScope, outcome.fields.isNotEmpty()))
                    pending.push(outcome.fields, anchorReference, if (anchorReference == null) null else holder)
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
            category(name)?.let {
                // A scalar has no field to select.
                return if (pending.isEmpty()) ProjectTypeDescription.builtIn(steps.values.toList(), it, reference, written)
                else pending.stop(steps, ProjectTypeDescription.AnchorStop.FIELD_NOT_FOUND, AnchorStopCause.FIELD_NOT_FOUND, written, trace)
            }
            val resolution = snapshot.resolveNamedType(name, owner)
            val reached = resolution.declaration.orElse(null)?.takeIf { resolution.status == ProjectNamedTypeResolution.Status.RESOLVED }
            if (!pending.isEmpty() && reached != null && reached !is ProjectSubtypeView) {
                if (steps.containsKey(reached.id)) return ProjectTypeDescription.cycle(steps.values.toList(), reference, written, pending.path())
                steps[reached.id] = reached
                val field = when (val member = snapshot.memberResolution(reached.id, pending.first())) {
                    is ProjectRecordMemberResolution.Resolved -> member
                    // A record or object type without that field, or a collection, whose only members are its methods.
                    is ProjectRecordMemberResolution.NotFound, is ProjectRecordMemberResolution.CollectionMethod ->
                        return pending.stop(steps, ProjectTypeDescription.AnchorStop.FIELD_NOT_FOUND, AnchorStopCause.FIELD_NOT_FOUND, written, trace)
                    else -> return pending.stop(steps, ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.FIELD_UNDECIDED, written, trace)
                }
                pending.select()
                val fieldType = ProjectTypeReference.from(field.field.typeRef)
                reference = fieldType
                name = fieldType.name
                // A field's written type is looked up from the package of the type that declares the field.
                owner = (field.declaration as? PackageTypeDeclaration)?.owner?.let(ProjectQualifiedName::wrap)
                holder = null
                holderKnown = false
                continue
            }
            terminalResolution(resolution, steps, reference, written, pending.path())?.let { return it }
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
        written: ProjectWrittenAnchor?,
        fieldPath: List<ProjectIdentifier>
    ): ProjectTypeDescription? {
        if (resolution.status != ProjectNamedTypeResolution.Status.RESOLVED) {
            return ProjectTypeDescription.unresolved(steps.values.toList(), resolution, reference, written, fieldPath)
        }
        val declaration = resolution.declaration.get()
        if (declaration !is ProjectSubtypeView) {
            return ProjectTypeDescription.declared(steps.values.toList(), declaration, reference, written)
        }
        if (steps.containsKey(declaration.id)) {
            return ProjectTypeDescription.cycle(steps.values.toList(), reference, written, fieldPath)
        }
        return null
    }

    /** One `%TYPE`/`%ROWTYPE` step of the walk, with its reference, the values followed so far, the pending fields and the trace. */
    private class AnchorStep(
        private val snapshot: ProjectSnapshot,
        private val reference: ProjectTypeReference?,
        private val steps: Map<ProjectDeclarationId, ProjectDeclarationView>,
        private val written: ProjectWrittenAnchor?,
        private val trace: AnchorTrace?,
        private val pending: PendingFields
    ) {
        /**
         * The anchor [anchorName] names: a stop, or the one source value it names with the fields its name selects
         * after the value. The step's reference is null only for the written request, which has no retained syntax;
         * the guards that need a retained holder (sibling capture, body scope, declaration order) are the consumer's
         * lexical gate for a written anchor and are skipped.
         */
        fun anchor(anchorName: ProjectQualifiedName, kind: ProjectTypeReference.Anchor, context: AnchorContext): Anchor {
            val holder = (context as? AnchorContext.Retained)?.holder
            trace?.holder = holder
            trace?.written = context is AnchorContext.Written
            trace?.cause = null
            trace?.captors = emptyList()
            // A remote anchor names another database: nothing in the project or the local metadata describes it.
            if (reference?.link?.isPresent == true) return stop(ProjectTypeDescription.AnchorStop.REMOTE, AnchorStopCause.REMOTE)
            // %ROWTYPE stops before any owner or scope guard, so a written one-segment %ROWTYPE without owner stays ROWTYPE.
            if (kind == ProjectTypeReference.Anchor.ROWTYPE) return stop(ProjectTypeDescription.AnchorStop.ROWTYPE, AnchorStopCause.ROWTYPE)
            if (context is AnchorContext.Retained && holder == null) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.NO_HOLDER)
            val segments = anchorName.name.segments
            holderStop(holder, segments)?.let { return it }
            val owner = when (context) {
                is AnchorContext.Retained -> holder!!.owner
                is AnchorContext.Written -> context.owner
            }
            packageScopeStop(owner, holder, segments)?.let { return it }
            fieldPath(owner, holder, segments)?.let { return it }
            val lookup = if (segments.size == 1) {
                snapshot.resolvePackageValue(anchorName, ProjectQualifiedName.wrap(owner!!))
            } else {
                snapshot.resolvePackageValue(anchorName, null)
            }
            if (lookup.status != ProjectValueResolution.Status.RESOLVED) {
                return Anchor.Stop(ProjectTypeDescription.unresolvedValue(followed(), lookup, reference, written, pending.path()))
            }
            return follow(lookup.value.get(), holder, owner, emptyList(), segments.size == 1)
        }

        private fun follow(value: ProjectPackageValueView, holder: ReferenceHolder?, owner: QualifiedName?, fields: List<OracleIdentifier>, packageScope: Boolean): Anchor {
            val index = snapshot.projectIndex
            // The resolved value must be the only binding of its package: several specifications are not one scope.
            if (index.findOccurrences(value.owner.name).count { val d = it.declaration; d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION } != 1) {
                return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_UNIT)
            }
            val valueOccurrence = snapshot.occurrenceOf(value.id) ?: return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_UNIT)
            declarationOrderStop(holder, owner, value, valueOccurrence)?.let { return it }
            if (steps.containsKey(value.id)) return Anchor.Cycle(value)
            return Anchor.Follow(value, fields, packageScope)
        }

        // The ordered copy is made only when a terminal description is built, never for a successful step.
        fun followed() = steps.values.toList()

        fun stop(why: ProjectTypeDescription.AnchorStop, cause: AnchorStopCause, captors: List<IndexedDeclaration> = emptyList()): Anchor.Stop {
            trace?.cause = cause
            trace?.captors = captors
            return Anchor.Stop(ProjectTypeDescription.anchorStopped(followed(), reference, why, written, pending.path()))
        }

        fun holderStop(holder: ReferenceHolder?, segments: List<OracleIdentifier>): Anchor.Stop? {
            if (holder == null) return null
            // A sibling record field or parameter captures the first segment before anything in the package.
            if (holder.capturingNames.any { it == segments.first() }) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.SIBLING)
            return null
        }

        fun packageScopeStop(owner: QualifiedName?, holder: ReferenceHolder?, segments: List<OracleIdentifier>): Anchor.Stop? {
            if (owner == null) {
                // Standalone declarations have no package scope: only a qualified name can name a package value.
                if (segments.size == 1) return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.NO_PACKAGE_SCOPE)
            } else {
                packageMemberStop(owner, segments)?.let { return it }
            }
            if (owner != null && holder?.role == DeclarationRole.BODY) bodyMemberStop(holder, segments)?.let { return it }
            return null
        }

        // Oracle rejects a reference to a body declaration of the name declared later, or to the subprogram containing it.
        private fun bodyMemberStop(holder: ReferenceHolder, segments: List<OracleIdentifier>): Anchor.Stop? {
            val index = snapshot.projectIndex
            val evidence = reference?.let { ReferenceHolders.bodyEvidence(index, holder, segments.first(), it.sourceRange) }
            return when (evidence?.status) {
                MemberNameStatus.NOT_DECLARED -> null
                MemberNameStatus.DECLARED_BEFORE ->
                    stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.BODY_MEMBER, ReferenceHolders.bodyMembers(index, holder, segments.first()))
                MemberNameStatus.REJECTED ->
                    stop(ProjectTypeDescription.AnchorStop.DECLARED_LATER, AnchorStopCause.DECLARED_LATER, ReferenceHolders.bodyMembers(index, holder, segments.first()))
                MemberNameStatus.UNKNOWN, null -> stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.BODY_SCOPE)
            }
        }

        private fun packageMemberStop(owner: QualifiedName, segments: List<OracleIdentifier>): Anchor.Stop? {
            // A declaration of the holder's package named like the first segment captures it (positive scope evidence,
            // independent of the preparation state): a value of the package starts a field path ([fieldPath]), and a
            // package declaration that is not a value makes the anchor invalid, so it never becomes a project lookup.
            // Values and other declarations of that name together leave the field path undecided.
            val captured = snapshot.projectIndex.findOccurrences(owner.append(segments.first()))
            val values = captured.count { it.declaration is PackageValueDeclaration }
            if (segments.size >= 2 && values < captured.size) {
                return stop(
                    if (values > 0) ProjectTypeDescription.AnchorStop.FIELD_PATH else ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE,
                    AnchorStopCause.PACKAGE_MEMBER, captured
                )
            }
            if (segments.size == 1 && captured.isNotEmpty() && values == 0) {
                return stop(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE, AnchorStopCause.PACKAGE_MEMBER, captured)
            }
            return null
        }

        /**
         * The value a field path starts from, followed with the fields its name selects after it: the value of the
         * holder's package named by the first segment (Oracle binds the first segment there before any schema object),
         * else the one qualified prefix that names a package value (`p.g.f`, `s.p.g.f`). Two prefixes that name a
         * value, a whole name that names one too, or a value not identified uniquely stop as a field path not
         * followed. Null when the name is no field path.
         */
        fun fieldPath(owner: QualifiedName?, holder: ReferenceHolder?, segments: List<OracleIdentifier>): Anchor? {
            if (segments.size < 2) return null
            val index = snapshot.projectIndex
            if (owner != null) {
                // packageMemberStop has stopped every capture that is not made of values only.
                val captured = index.findOccurrences(owner.append(segments.first()))
                if (captured.isNotEmpty()) {
                    val lookup = snapshot.resolvePackageValue(ProjectQualifiedName.wrap(QualifiedName(segments.subList(0, 1))), ProjectQualifiedName.wrap(owner))
                    if (lookup.status != ProjectValueResolution.Status.RESOLVED) {
                        return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.PACKAGE_MEMBER, captured)
                    }
                    return follow(lookup.value.get(), holder, owner, segments.subList(1, segments.size), true)
                }
            }
            var length = 0
            var values: List<IndexedDeclaration> = emptyList()
            for (size in 2 until segments.size) {
                val found = index.findOccurrences(QualifiedName(segments.subList(0, size))).filter { it.declaration is PackageValueDeclaration }
                if (found.isEmpty()) continue
                if (length != 0) return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.PREFIX_VALUE, values + found)
                length = size
                values = found
            }
            if (length == 0) return null
            if (index.findOccurrences(QualifiedName(segments)).any { it.declaration is PackageValueDeclaration }) {
                return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.PREFIX_VALUE, values)
            }
            val lookup = snapshot.resolvePackageValue(ProjectQualifiedName.wrap(QualifiedName(segments.subList(0, length))), null)
            if (lookup.status != ProjectValueResolution.Status.RESOLVED) {
                return stop(ProjectTypeDescription.AnchorStop.FIELD_PATH, AnchorStopCause.PREFIX_VALUE, values)
            }
            return follow(lookup.value.get(), holder, owner, segments.subList(length, segments.size), false)
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

    private fun category(name: ProjectQualifiedName): ProjectBuiltInTypeCategory? = BuiltInTypeRefClassifier.classify(name.name)
}
