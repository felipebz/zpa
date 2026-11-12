//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import com.felipebz.zpa.project.*

/**
 * Composes one source walk of [ProjectTypeDescriber] with exact external lookups, in one fixed decision order. The
 * source side is never re-resolved: values, subtypes and anchor scope come from the walk and from the facts it already
 * used (its stop record, reference holders, package member-name coverage). Nothing is cached or retained.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectExternalTypeDescriber {
    @JvmSynthetic
    fun describe(
        snapshot: ProjectSnapshot,
        startName: ProjectQualifiedName,
        startReference: ProjectTypeReference?,
        packageOwner: ProjectQualifiedName?,
        written: ProjectWrittenAnchor?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectExternalTypeDescription {
        val trace = AnchorTrace()
        val source = ProjectTypeDescriber.describe(snapshot, startName, startReference, packageOwner, written, trace)
        return Composition(snapshot, source, startReference, written, packageOwner, metadata, currentSchema, trace).compose()
    }
}

@OptIn(ZpaExperimentalApi::class)
private class Composition(
    private val snapshot: ProjectSnapshot,
    private val source: ProjectTypeDescription,
    private val startReference: ProjectTypeReference?,
    private val written: ProjectWrittenAnchor?,
    private val packageOwner: ProjectQualifiedName?,
    private val metadata: ProjectExternalMetadata,
    private val currentSchema: ProjectIdentifier?,
    private val trace: AnchorTrace
) {
    private val index = snapshot.projectIndex
    private val available = metadata.origin.isPresent

    /** Holder of the start reference, located once: the search scans the retained references of its file. */
    private val startHolder = startReference?.let { ReferenceHolders.find(index, it) }

    /**
     * An anchor written somewhere in the walk: its name, kind, and whether it is a retained reference. An anchor the
     * walk stopped at with a pending field path is read with that path appended ([fields] segments) as a `%TYPE`
     * anchor of kind [base] before the path: `customer.name%TYPE` for the field `name` of a `customer%ROWTYPE` value.
     */
    private class WrittenName(
        val name: ProjectQualifiedName,
        val kind: ProjectTypeReference.Anchor,
        val reference: ProjectTypeReference?,
        val fields: Int = 0,
        val base: ProjectTypeReference.Anchor = kind
    )

    /** The relation reading of an anchor shape: unqualified readings need [currentSchema]. */
    private class Reading(val qualified: Boolean, val schema: ProjectIdentifier?, val relation: ProjectIdentifier, val column: ProjectIdentifier?) {
        val name: ProjectQualifiedName? get() = schema?.let { ProjectQualifiedName.ofIdentifiers(listOf(it, relation)) }
    }

    private class Outcome(
        val status: Status,
        val anchor: WrittenName? = null,
        val relationName: ProjectQualifiedName? = null,
        val lookup: ProjectExternalLookup? = null,
        val sourceCandidates: List<ProjectDeclarationView> = emptyList(),
        val externalCandidates: List<ProjectExternalRelation> = emptyList(),
        /** Facts a definitive answer rests on; any conditionally compiled one makes it inconclusive. */
        val dependencies: List<ProjectDeclaration> = emptyList(),
        val synonyms: List<ProjectExternalSynonym> = emptyList(),
        val capturingObject: ProjectExternalObject? = null
    )

    fun compose(): ProjectExternalTypeDescription {
        val outcome = decide()
        return guarded(outcome).let {
            ProjectExternalTypeDescription.of(
                it.status, source, it.anchor?.kind, it.anchor?.name, it.relationName, it.lookup, distinct(it.sourceCandidates),
                it.externalCandidates.distinct(), it.synonyms, it.capturingObject
            )
        }
    }

    /** Step 5 result: a decided outcome, or the open scope (owner and whether a capture is not excluded). */
    private sealed interface ScopeResult {
        class Decided(val outcome: Outcome) : ScopeResult
        class Open(val owner: QualifiedName?, val uncertain: Boolean) : ScopeResult
    }

    private fun decide(): Outcome {
        val stepFacts = source.steps.mapNotNull { factOf(it) }
        val walkFacts = stepFacts + listOfNotNull(source.declaration.orElse(null)?.let { factOf(it) })
        val referenceFacts = listOfNotNull(startHolder?.fact)

        // A remote anchor is not described by the local metadata: no lookup, no competition.
        if (source.anchorStop.orElse(null) == ProjectTypeDescription.AnchorStop.REMOTE) return Outcome(Status.REMOTE, stoppedAnchor())

        // 2. A followed anchor is a positive source interpretation; a found relation reading of it competes.
        followedConflict(stepFacts, referenceFacts)?.let { return it }

        // 3. Terminal classification.
        val stopped = stoppedAnchor()
        terminalOutcome(stopped, stepFacts, walkFacts, referenceFacts)?.let { return it }
        val anchor = stopped ?: return Outcome(Status.NOT_EXTERNAL, dependencies = walkFacts)

        // 4. Shape.
        val reading = reading(anchor) ?: return Outcome(Status.NOT_EXTERNAL, anchor)
        val first = anchor.name.segments.first()

        // 5. Proven source capture, or a same-named member that makes Oracle reject the anchor.
        val scope = when (val result = sourceScope(anchor, first, stepFacts)) {
            is ScopeResult.Decided -> return result.outcome
            is ScopeResult.Open -> result
        }

        val captors = possibleCaptors(anchor, scope.owner)
        return outcomeAfterScope(anchor, reading, first, captors, scope.uncertain, stepFacts, referenceFacts)
    }

    /** Step 2. A value bound in package scope (a one-segment name, or the start of a field path there) has no relation reading. */
    private fun followedConflict(stepFacts: List<ProjectDeclaration>, referenceFacts: List<ProjectDeclaration>): Outcome? {
        if (!available) return null
        val followed = trace.followed.filter { !it.packageScope }
        val (anchor, found) = followed.firstNotNullOfOrNull { followedAnchor ->
            competing(followedAnchor.name).takeIf { it.isNotEmpty() }?.let { written(followedAnchor) to it }
        } ?: return null
        val values = followed.filter { competing(it.name).isNotEmpty() }.map { it.value }
        return conflict(anchor, values, found, stepFacts + referenceFacts)
    }

    /** Step 3: a terminal outcome, or null when the classification continues with step 4. */
    private fun terminalOutcome(
        stopped: WrittenName?,
        stepFacts: List<ProjectDeclaration>,
        walkFacts: List<ProjectDeclaration>,
        referenceFacts: List<ProjectDeclaration>
    ): Outcome? {
        when (source.status) {
            ProjectTypeDescription.Status.ANCHORED_REFERENCE -> when (trace.cause) {
                AnchorStopCause.PREFIX_VALUE -> return prefixValueOutcome(stopped, stepFacts, referenceFacts)
                AnchorStopCause.DECLARED_LATER ->
                    return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = snapshot.viewsOf(trace.captors),
                        dependencies = stepFacts + trace.captors.map { it.declaration })
                // The first segment bound to a package value: the anchor is a field path, never a relation reading.
                // The type examined (or the value of a scalar type) is the declaration that decides it.
                AnchorStopCause.FIELD_NOT_FOUND, AnchorStopCause.FIELD_UNDECIDED ->
                    return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = listOfNotNull(source.steps.lastOrNull()), dependencies = stepFacts)
                else -> Unit
            }
            ProjectTypeDescription.Status.UNRESOLVED_VALUE -> {
                val values = source.valueResolution.get()
                if (values.status == ProjectValueResolution.Status.AMBIGUOUS) {
                    return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = values.candidates,
                        dependencies = stepFacts + values.candidates.mapNotNull { factOf(it) })
                }
            }
            else -> return Outcome(Status.NOT_EXTERNAL, dependencies = walkFacts)
        }
        return null
    }

    /** Step 3, an anchor stopped by a shorter qualified package value. */
    private fun prefixValueOutcome(stopped: WrittenName?, stepFacts: List<ProjectDeclaration>, referenceFacts: List<ProjectDeclaration>): Outcome {
        val values = snapshot.viewsOf(trace.captors)
        val found = if (available && stopped != null) competing(stopped.name) else emptyList()
        if (found.isNotEmpty()) return conflict(stopped!!, values, found, stepFacts + trace.captors.map { it.declaration } + referenceFacts)
        return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = values, dependencies = stepFacts + trace.captors.map { it.declaration })
    }

    /** Step 5. */
    private fun sourceScope(anchor: WrittenName, first: ProjectIdentifier, stepFacts: List<ProjectDeclaration>): ScopeResult {
        var uncertain = trace.cause == AnchorStopCause.NO_HOLDER || trace.cause == AnchorStopCause.PACKAGE_UNIT
        val holder = if (trace.written) null else trace.holder
        if (!trace.written && holder == null) uncertain = true
        if (holder != null && holder.capturingNames.contains(first.identifier)) {
            return ScopeResult.Decided(Outcome(Status.NOT_EXTERNAL, anchor, sourceCandidates = viewOf(holder.fact), dependencies = stepFacts))
        }
        val owner = if (trace.written) packageOwner?.name else holder?.owner
        val member = owner?.let { packageMemberScope(it, holder, anchor, first) }
        if (member is ScopeResult.Decided) return member
        if (member is ScopeResult.Open && member.uncertain) uncertain = true
        return ScopeResult.Open(owner, uncertain)
    }

    /** Step 5, the holder's package: whether this check alone makes the scope uncertain. */
    private fun packageMemberScope(owner: QualifiedName, holder: ReferenceHolder?, anchor: WrittenName, first: ProjectIdentifier): ScopeResult {
        val retainedInSpec = holder != null && holder.role == DeclarationRole.SPECIFICATION
        val unit = if (retainedInSpec) holder.unit else singleSpecification(owner)
        var uncertain = unit == null
        if (unit != null) {
            when (val evidence = memberNameEvidence(owner, unit, retainedInSpec, anchor, first)) {
                is ScopeResult.Decided -> return evidence
                is ScopeResult.Open -> uncertain = evidence.uncertain
            }
        }
        if (holder?.role == DeclarationRole.BODY) {
            when (val body = bodyScope(holder, anchor, first)) {
                is ScopeResult.Decided -> return body
                is ScopeResult.Open -> if (body.uncertain) uncertain = true
            }
        }
        return ScopeResult.Open(owner, uncertain)
    }

    /** Step 5, the package body's own level: Oracle rejects a reference to a body declaration of the name declared later too. */
    private fun bodyScope(holder: ReferenceHolder, anchor: WrittenName, first: ProjectIdentifier): ScopeResult {
        val evidence = anchor.reference?.sourceRange?.let { ReferenceHolders.bodyEvidence(index, holder, first.identifier, it) }
        return when (evidence?.status) {
            MemberNameStatus.DECLARED_BEFORE, MemberNameStatus.REJECTED -> {
                val members = snapshot.viewsOf(ReferenceHolders.bodyMembers(index, holder, first.identifier))
                ScopeResult.Decided(Outcome(Status.NOT_EXTERNAL, anchor, sourceCandidates = members, dependencies = members.mapNotNull { factOf(it) }))
            }
            MemberNameStatus.NOT_DECLARED -> ScopeResult.Open(holder.owner, false)
            MemberNameStatus.UNKNOWN, null -> ScopeResult.Open(holder.owner, true)
        }
    }

    /** Step 5, the package member-name evidence for the first segment in [unit]. */
    private fun memberNameEvidence(owner: QualifiedName, unit: IndexedDeclaration, retainedInSpec: Boolean, anchor: WrittenName, first: ProjectIdentifier): ScopeResult {
        val position = if (retainedInSpec) anchor.reference?.sourceRange?.let(::internalRange) else null
        val evidence = PackageMemberNames.at(index, unit, first.identifier, position)
        return when (evidence.status) {
            MemberNameStatus.DECLARED_BEFORE, MemberNameStatus.REJECTED -> {
                val members = memberViews(owner, first.identifier, unit, evidence)
                ScopeResult.Decided(Outcome(Status.NOT_EXTERNAL, anchor, sourceCandidates = members, dependencies = members.mapNotNull { factOf(it) }))
            }
            MemberNameStatus.NOT_DECLARED -> ScopeResult.Open(owner, trace.cause == AnchorStopCause.PACKAGE_MEMBER)
            MemberNameStatus.UNKNOWN -> ScopeResult.Open(owner, true)
        }
    }

    /** Steps 6–13. */
    private fun outcomeAfterScope(
        anchor: WrittenName,
        reading: Reading,
        first: ProjectIdentifier,
        captors: List<ProjectDeclarationView>,
        uncertain: Boolean,
        stepFacts: List<ProjectDeclaration>,
        referenceFacts: List<ProjectDeclaration>
    ): Outcome {
        // 6. Source completeness.
        if (snapshot.failures.isNotEmpty() || snapshot.preparationState == ProjectPreparationState.NOT_PREPARED) {
            return inconclusive(Status.SOURCE_INCOMPLETE, anchor, reading, captors)
        }
        // 7. Uncertain scope.
        if (uncertain) {
            return inconclusive(Status.CAPTURE_NOT_EXCLUDED, anchor, reading, captors + stepFacts.filter(::conditional).flatMap { viewOf(it) })
        }
        // 8. Availability.
        if (!available) {
            return Outcome(Status.UNAVAILABLE, anchor, reading.name, reading.name?.let { lookup(reading) }, captors)
        }
        // 9. Context.
        if (!reading.qualified && reading.schema == null) return Outcome(Status.SCHEMA_CONTEXT_MISSING, anchor, sourceCandidates = captors)
        // 10. Name resolution and lookups.
        if (reading.qualified) return qualifiedOutcome(anchor, reading, first, captors, stepFacts + referenceFacts)
        return readingOutcome(anchor, reading, resolve(reading.schema!!, reading.relation, unqualified = true), captors, stepFacts + referenceFacts)
    }

    /**
     * Step 11. Oracle reads the first segment of a schema-qualified anchor as an object of the compilation schema, then
     * as a public synonym, and only when neither takes it as a schema.
     */
    private fun qualifiedOutcome(
        anchor: WrittenName,
        reading: Reading,
        first: ProjectIdentifier,
        captors: List<ProjectDeclarationView>,
        dependencies: List<ProjectDeclaration>
    ): Outcome {
        val compiling = currentSchema
        val captor = compiling?.let { metadata.findObject(it, first).orElse(null) }
            ?: metadata.findObject(ProjectExternalSynonym.PUBLIC, first).orElse(null)
        if (captor != null) return Outcome(Status.NOT_EXTERNAL, anchor, capturingObject = captor)
        if (compiling == null || !metadata.isSchemaComplete(compiling) || !metadata.isPublicSynonymsComplete) {
            return inconclusive(Status.CAPTURE_NOT_EXCLUDED, anchor, reading, captors)
        }
        return readingOutcome(anchor, reading, resolve(reading.schema!!, reading.relation, unqualified = false), captors, dependencies)
    }

    private sealed interface Target {
        class Relation(val schema: ProjectIdentifier, val name: ProjectIdentifier) : Target
        class Absent(val schema: ProjectIdentifier, val name: ProjectIdentifier, val proven: Boolean) : Target
        class Other(val schemaObject: ProjectExternalObject) : Target
        data object Remote : Target
        data object Cycle : Target
    }

    private class Resolution(val target: Target, val synonyms: List<ProjectExternalSynonym>)

    /**
     * What the relation name [name] of [schema] stands for, as Oracle resolves it: an object of the schema, then (for
     * an [unqualified] name only) a public synonym; synonyms are followed to their target. Absence is proven only for
     * the written name itself in a complete schema, with complete public synonyms for an unqualified name: a synonym
     * whose target is missing from the metadata may point to an object the session cannot see.
     */
    private fun resolve(schema: ProjectIdentifier, name: ProjectIdentifier, unqualified: Boolean): Resolution {
        val synonyms = ArrayList<ProjectExternalSynonym>()
        var owner = schema
        var current = name
        var public = unqualified
        while (true) {
            val found = metadata.findObject(owner, current).orElse(null)
            val synonym = when {
                found == null && public && metadata.isSchemaComplete(owner) ->
                    metadata.findSynonym(ProjectExternalSynonym.PUBLIC, current).orElse(null)
                        ?: return Resolution(Target.Absent(owner, current, metadata.isPublicSynonymsComplete), synonyms)
                found == null ->
                    return Resolution(Target.Absent(owner, current, !unqualified && synonyms.isEmpty() && metadata.isSchemaComplete(owner)), synonyms)
                found.kind == ProjectExternalObject.Kind.SYNONYM ->
                    metadata.findSynonym(owner, current).orElse(null) ?: return Resolution(Target.Absent(owner, current, false), synonyms)
                found.kind == ProjectExternalObject.Kind.TABLE || found.kind == ProjectExternalObject.Kind.VIEW ->
                    return Resolution(Target.Relation(owner, current), synonyms)
                else -> return Resolution(Target.Other(found), synonyms)
            }
            // Oracle rejects a synonym that resolves to itself (ORA-01775).
            if (synonym in synonyms) return Resolution(Target.Cycle, synonyms)
            synonyms += synonym
            if (synonym.dbLink.isPresent) return Resolution(Target.Remote, synonyms)
            owner = synonym.targetOwner.get()
            current = synonym.targetName
            public = false
        }
    }

    /** Steps 12 and 13. */
    private fun readingOutcome(
        anchor: WrittenName,
        reading: Reading,
        resolution: Resolution,
        captors: List<ProjectDeclarationView>,
        dependencies: List<ProjectDeclaration>
    ): Outcome {
        val synonyms = resolution.synonyms
        val (schema, name) = when (val target = resolution.target) {
            Target.Remote -> return Outcome(Status.REMOTE, anchor, synonyms = synonyms)
            Target.Cycle -> return Outcome(Status.NOT_EXTERNAL, anchor, synonyms = synonyms)
            is Target.Other -> return Outcome(Status.NOT_EXTERNAL, anchor, capturingObject = target.schemaObject, synonyms = synonyms)
            is Target.Absent -> {
                val relationName = qualified(target.schema, target.name)
                if (target.proven) {
                    return Outcome(Status.RELATION_NOT_FOUND, anchor, relationName, lookup(target.schema, target.name, reading.column), captors, synonyms = synonyms)
                }
                return Outcome(Status.INCOMPLETE, anchor, relationName, notCovered(target.schema, target.name, reading.column), captors, synonyms = synonyms)
            }
            is Target.Relation -> target.schema to target.name
        }
        val relationName = qualified(schema, name)
        // An object named as a table or view whose relation the metadata does not supply is not covered.
        val lookup = lookup(schema, name, reading.column).takeIf { it.status != ProjectExternalLookup.Status.RELATION_NOT_FOUND }
            ?: notCovered(schema, name, reading.column)
        when (lookup.status) {
            ProjectExternalLookup.Status.NOT_COVERED -> return Outcome(Status.INCOMPLETE, anchor, relationName, lookup, captors, synonyms = synonyms)
            ProjectExternalLookup.Status.COLUMN_NOT_FOUND -> return Outcome(Status.COLUMN_NOT_FOUND, anchor, relationName, lookup, captors, synonyms = synonyms)
            else -> Unit
        }
        if (reading.column == null && !lookup.relation.get().isColumnsComplete) {
            return Outcome(Status.INCOMPLETE, anchor, relationName, lookup, captors, synonyms = synonyms)
        }
        return Outcome(Status.RESOLVED, anchor, relationName, lookup, dependencies = dependencies, synonyms = synonyms)
    }

    private fun qualified(schema: ProjectIdentifier, name: ProjectIdentifier) = ProjectQualifiedName.ofIdentifiers(listOf(schema, name))

    private fun lookup(schema: ProjectIdentifier, name: ProjectIdentifier, column: ProjectIdentifier?): ProjectExternalLookup =
        if (column == null) metadata.findRelation(schema, name) else metadata.findColumn(schema, name, column)

    private fun notCovered(schema: ProjectIdentifier, name: ProjectIdentifier, column: ProjectIdentifier?) =
        ProjectExternalLookup.of(ProjectExternalLookup.Status.NOT_COVERED, schema, name, column, null, null, metadata.origin.get())

    /**
     * A definitive answer (`RESOLVED`, `NOT_EXTERNAL`, `CONFLICT`) that rests on a conditionally compiled declaration
     * is not proven for every compilation: it becomes `CAPTURE_NOT_EXCLUDED`, keeping its evidence and listing the
     * conditional declarations among the source candidates. Every definitive answer rests on the walk's provenance
     * ([provenance]) plus the outcome's own captors or members. Without a stopped anchor, the relation reading kept is
     * that of the first followed field path that has one (`employees.salary%TYPE` through a conditional value
     * `employees`): the reading Oracle makes when the value is not compiled.
     */
    private fun guarded(outcome: Outcome): Outcome {
        if (outcome.status != Status.RESOLVED && outcome.status != Status.NOT_EXTERNAL && outcome.status != Status.CONFLICT) return outcome
        val conditional = (provenance() + outcome.dependencies).filter(::conditional).distinct()
        if (conditional.isEmpty()) return outcome
        val anchor = outcome.anchor ?: stoppedAnchor()
            ?: trace.followed.firstNotNullOfOrNull { followed -> written(followed).takeIf { followed.selectsFields && reading(it) != null } }
        val reading = if (outcome.relationName == null) anchor?.let { reading(it) } else null
        val lookup = outcome.lookup ?: reading?.takeIf { available && it.name != null }?.let { lookup(it) }
        val relationName = outcome.relationName ?: lookup?.let { reading!!.name }
        val captors = if (outcome.status == Status.RESOLVED && anchor != null) possibleCaptors(anchor, ownerOfAnchor()) else outcome.sourceCandidates
        return Outcome(
            Status.CAPTURE_NOT_EXCLUDED, anchor, relationName, lookup,
            captors + conditional.flatMap { viewOf(it) }, outcome.externalCandidates, synonyms = outcome.synonyms, capturingObject = outcome.capturingObject
        )
    }

    /**
     * The source facts every result of the walk rests on: the declaration retaining the start reference, the one
     * retaining the terminal anchor (from the trace; none for the written request, whose lexical context is the
     * caller's), the followed values and subtypes, and the reached declaration.
     */
    private fun provenance(): List<ProjectDeclaration> =
        listOfNotNull(
            startHolder?.fact,
            if (trace.written) null else trace.holder?.fact,
            source.declaration.orElse(null)?.let { factOf(it) }
        ) + source.steps.mapNotNull { factOf(it) }

    private fun conflict(anchor: WrittenName, values: List<ProjectDeclarationView>, found: List<ProjectExternalLookup>, dependencies: List<ProjectDeclaration>) =
        Outcome(
            Status.CONFLICT, anchor, found.first().relation.get().qualifiedName, found.first(), values,
            found.map { it.relation.get() }, dependencies
        )

    private fun inconclusive(status: Status, anchor: WrittenName, reading: Reading, captors: List<ProjectDeclarationView>): Outcome {
        val lookup = if (available && reading.name != null) lookup(reading) else null
        return Outcome(status, anchor, reading.name, lookup, captors)
    }

    private fun written(followed: FollowedAnchor) = WrittenName(followed.name, ProjectTypeReference.Anchor.TYPE, followed.reference)

    /** Found relation readings of a followed anchor with at least two segments (a one-segment anchor binds in package scope). */
    private fun competing(name: ProjectQualifiedName): List<ProjectExternalLookup> {
        val segments = name.segments
        if (segments.size < 2) return emptyList()
        val found = ArrayList<ProjectExternalLookup>()
        if (currentSchema != null) found += metadata.findRelation(currentSchema, segments[0])
        if (segments.size >= 3) found += metadata.findRelation(segments[0], segments[1])
        return found.filter { it.status == ProjectExternalLookup.Status.FOUND }
    }

    /**
     * The anchor the walk stopped at: the terminal reference, else the written request, with the fields an enclosing
     * anchor still selects past it ([ProjectTypeDescription.fieldPath]) appended.
     */
    private fun stoppedAnchor(): WrittenName? {
        if (source.status != ProjectTypeDescription.Status.ANCHORED_REFERENCE && source.status != ProjectTypeDescription.Status.UNRESOLVED_VALUE) return null
        val terminal = source.terminalReference.orElse(null)
        val anchor = if (terminal != null) {
            if (terminal.kind == ProjectTypeReference.Kind.ANCHORED) WrittenName(terminal.name, terminal.anchor.get(), terminal) else null
        } else {
            written?.let { WrittenName(it.name, it.anchor, null) }
        }
        val path = source.fieldPath
        if (anchor == null || path.isEmpty()) return anchor
        val name = ProjectQualifiedName.ofIdentifiers(anchor.name.segments + path)
        return WrittenName(name, ProjectTypeReference.Anchor.TYPE, anchor.reference, path.size, anchor.kind)
    }

    private fun ownerOfAnchor(): QualifiedName? = if (trace.written) packageOwner?.name else trace.holder?.owner

    /**
     * The relation reading of an anchor shape. A field path after a `%ROWTYPE` anchor reads as one column of that
     * relation; any other field path (after a column, or more than one field) is no shape the metadata describes.
     */
    private fun reading(anchor: WrittenName): Reading? {
        if (anchor.fields > 0 && (anchor.base != ProjectTypeReference.Anchor.ROWTYPE || anchor.fields != 1)) return null
        val s = anchor.name.segments
        return when {
            anchor.kind == ProjectTypeReference.Anchor.ROWTYPE && s.size == 1 -> Reading(false, currentSchema, s[0], null)
            anchor.kind == ProjectTypeReference.Anchor.TYPE && s.size == 2 -> Reading(false, currentSchema, s[0], s[1])
            anchor.kind == ProjectTypeReference.Anchor.ROWTYPE && s.size == 2 -> Reading(true, s[0], s[1], null)
            anchor.kind == ProjectTypeReference.Anchor.TYPE && s.size == 3 -> Reading(true, s[0], s[1], s[2])
            else -> null
        }
    }

    private fun lookup(reading: Reading): ProjectExternalLookup {
        val schema = reading.schema!!
        return if (reading.column == null) metadata.findRelation(schema, reading.relation)
        else metadata.findColumn(schema, reading.relation, reading.column)
    }

    /**
     * Project declarations that may capture the anchor: those named like a proper prefix of the anchor name (and the
     * whole name for `%ROWTYPE`), members of the package scope named like the first segment, and the non-value
     * candidates of the stopped value lookup.
     */
    private fun possibleCaptors(anchor: WrittenName, owner: QualifiedName?): List<ProjectDeclarationView> {
        val segments = anchor.name.name.segments
        val last = if (anchor.kind == ProjectTypeReference.Anchor.ROWTYPE) segments.size else segments.size - 1
        val names = (1..last).map { QualifiedName(segments.subList(0, it)) } + listOfNotNull(owner?.append(segments.first()))
        val result = ArrayList<ProjectDeclarationView>()
        for (name in names) result += snapshot.viewsOf(index.findOccurrences(name))
        source.valueResolution.ifPresent { values -> result += values.candidates.filter { it !is ProjectPackageValueView } }
        return distinct(result)
    }

    private fun singleSpecification(owner: QualifiedName): IndexedDeclaration? = index.findOccurrences(owner).singleOrNull {
        val d = it.declaration
        d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION
    }

    private fun memberViews(owner: QualifiedName, name: OracleIdentifier, unit: IndexedDeclaration, evidence: MemberNameEvidence): List<ProjectDeclarationView> {
        val fileId = unit.declaration.fileId
        return snapshot.viewsOf(index.findOccurrences(owner.append(name)).filter {
            it.declaration !is PackageDeclaration && it.declaration.fileId == fileId && it.declaration.sourceRange in evidence.members
        })
    }

    /** True when [fact] is a specification member whose declaration overlaps a conditional-compilation span (or whose span is unknown). */
    private fun conditional(fact: ProjectDeclaration): Boolean {
        if (fact.role != DeclarationRole.SPECIFICATION) return false
        val owner = ReferenceHolders.ownerOf(fact) ?: return false
        val unit = ReferenceHolders.unitOf(index, fact, owner) ?: return true
        val coverage = (unit.declaration as PackageDeclaration).memberNames ?: return true
        return !coverage.isUnconditional(fact.sourceRange)
    }

    private fun factOf(view: ProjectDeclarationView): ProjectDeclaration? = snapshot.occurrenceOf(view.id)?.declaration

    private fun viewOf(fact: ProjectDeclaration): List<ProjectDeclarationView> =
        snapshot.viewsOf(index.occurrencesFor(fact.fileId).filter { it.declaration === fact })

    private fun distinct(views: List<ProjectDeclarationView>): List<ProjectDeclarationView> = views.distinctBy { it.id }

    private fun internalRange(range: ProjectSourceRange) =
        SourceRange(FileId(range.fileId), range.startLine, range.startColumn, range.endLine, range.endColumn)
}
