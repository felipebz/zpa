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

    /** An anchor written somewhere in the walk: its name, kind, and whether it is a retained reference. */
    private class WrittenName(val name: ProjectQualifiedName, val kind: ProjectTypeReference.Anchor, val reference: ProjectTypeReference?)

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
        val dependencies: List<ProjectDeclaration> = emptyList()
    )

    fun compose(): ProjectExternalTypeDescription {
        val outcome = decide()
        return guarded(outcome).let {
            ProjectExternalTypeDescription.of(
                it.status, source, it.anchor?.kind, it.anchor?.name, it.relationName, it.lookup, distinct(it.sourceCandidates), it.externalCandidates.distinct()
            )
        }
    }

    private fun decide(): Outcome {
        val stepFacts = source.steps.mapNotNull { factOf(it) }
        val walkFacts = stepFacts + listOfNotNull(source.declaration.orElse(null)?.let { factOf(it) })
        val referenceFacts = listOfNotNull(startHolder?.fact)

        // 2. A followed anchor is a positive source interpretation; a found relation reading of it competes.
        if (available) {
            followedAnchors().firstNotNullOfOrNull { (anchor, value) ->
                competing(anchor.name).takeIf { it.isNotEmpty() }?.let { found -> Triple(anchor, value, found) }
            }?.let { (anchor, _, found) ->
                val values = followedAnchors().filter { competing(it.first.name).isNotEmpty() }.map { it.second }
                return conflict(anchor, values, found, stepFacts + referenceFacts)
            }
        }

        // 3. Terminal classification.
        val stopped = stoppedAnchor()
        when (source.status) {
            ProjectTypeDescription.Status.ANCHORED_REFERENCE -> when (trace.cause) {
                AnchorStopCause.PREFIX_VALUE -> {
                    val values = snapshot.viewsOf(trace.captors)
                    val found = if (available && stopped != null) competing(stopped.name) else emptyList()
                    if (found.isNotEmpty()) return conflict(stopped!!, values, found, stepFacts + trace.captors.map { it.declaration } + referenceFacts)
                    return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = values, dependencies = stepFacts + trace.captors.map { it.declaration })
                }
                AnchorStopCause.DECLARED_LATER ->
                    return Outcome(Status.NOT_EXTERNAL, stopped, sourceCandidates = snapshot.viewsOf(trace.captors),
                        dependencies = stepFacts + trace.captors.map { it.declaration })
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
        val anchor = stopped ?: return Outcome(Status.NOT_EXTERNAL, dependencies = walkFacts)

        // 4. Shape.
        val reading = reading(anchor) ?: return Outcome(Status.NOT_EXTERNAL, anchor)
        val first = anchor.name.segments.first()

        // 5. Proven source capture, or a same-named member that makes Oracle reject the anchor.
        var uncertain = trace.cause == AnchorStopCause.NO_HOLDER || trace.cause == AnchorStopCause.PACKAGE_UNIT
        val holder = if (trace.written) null else trace.holder
        if (!trace.written && holder == null) uncertain = true
        if (holder != null && holder.capturingNames.contains(first.identifier)) {
            return Outcome(Status.NOT_EXTERNAL, anchor, sourceCandidates = viewOf(holder.fact), dependencies = stepFacts)
        }
        val owner = if (trace.written) packageOwner?.name else holder?.owner
        if (owner != null) {
            val retainedInSpec = holder != null && holder.role == DeclarationRole.SPECIFICATION
            val unit = if (retainedInSpec) holder.unit else singleSpecification(owner)
            if (unit == null) {
                uncertain = true
            } else {
                val position = if (retainedInSpec) anchor.reference?.sourceRange?.let(::internalRange) else null
                val evidence = PackageMemberNames.at(index, unit, first.identifier, position)
                when (evidence.status) {
                    MemberNameStatus.DECLARED_BEFORE, MemberNameStatus.REJECTED -> {
                        val members = memberViews(owner, first.identifier, unit, evidence)
                        return Outcome(Status.NOT_EXTERNAL, anchor, sourceCandidates = members, dependencies = members.mapNotNull { factOf(it) })
                    }
                    MemberNameStatus.NOT_DECLARED -> if (trace.cause == AnchorStopCause.PACKAGE_MEMBER) uncertain = true
                    MemberNameStatus.UNKNOWN -> uncertain = true
                }
            }
            if (holder?.role == DeclarationRole.BODY) uncertain = true
        }

        val captors = possibleCaptors(anchor, owner)
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
        // 10. Lookups.
        val lookup = lookup(reading)
        val relationName = reading.name!!
        // 11. Schema-qualified readings: a current-schema relation named like the qualifier captures it, or competes.
        if (reading.qualified && currentSchema != null) {
            val current = metadata.findRelation(currentSchema, first)
            if (current.status == ProjectExternalLookup.Status.FOUND) {
                val currentRelation = current.relation.get()
                if (lookup.status == ProjectExternalLookup.Status.FOUND) {
                    return Outcome(Status.AMBIGUOUS, anchor, relationName, lookup, captors, listOf(currentRelation, lookup.relation.get()))
                }
                return Outcome(Status.CAPTURE_NOT_EXCLUDED, anchor, relationName, lookup, captors, listOf(currentRelation))
            }
        }
        // 12. Lookup outcome.
        when (lookup.status) {
            ProjectExternalLookup.Status.NOT_COVERED -> return Outcome(Status.INCOMPLETE, anchor, relationName, lookup, captors)
            ProjectExternalLookup.Status.RELATION_NOT_FOUND -> return Outcome(Status.RELATION_NOT_FOUND, anchor, relationName, lookup, captors)
            ProjectExternalLookup.Status.COLUMN_NOT_FOUND -> return Outcome(Status.COLUMN_NOT_FOUND, anchor, relationName, lookup, captors)
            else -> Unit
        }
        if (reading.column == null && !lookup.relation.get().isColumnsComplete) {
            return Outcome(Status.INCOMPLETE, anchor, relationName, lookup, captors)
        }
        // 13. Final.
        if (reading.qualified) return Outcome(Status.CAPTURE_NOT_EXCLUDED, anchor, relationName, lookup, captors)
        return Outcome(Status.RESOLVED, anchor, relationName, lookup, dependencies = stepFacts + referenceFacts)
    }

    /**
     * A definitive answer (`RESOLVED`, `NOT_EXTERNAL`, `CONFLICT`) that rests on a conditionally compiled declaration
     * is not proven for every compilation: it becomes `CAPTURE_NOT_EXCLUDED`, keeping its evidence and listing the
     * conditional declarations among the source candidates. Every definitive answer rests on the walk's provenance
     * ([provenance]) plus the outcome's own captors or members.
     */
    private fun guarded(outcome: Outcome): Outcome {
        if (outcome.status != Status.RESOLVED && outcome.status != Status.NOT_EXTERNAL && outcome.status != Status.CONFLICT) return outcome
        val conditional = (provenance() + outcome.dependencies).distinct().filter(::conditional)
        if (conditional.isEmpty()) return outcome
        val anchor = outcome.anchor ?: stoppedAnchor()
        val reading = if (outcome.relationName == null) anchor?.let { reading(it) } else null
        val lookup = outcome.lookup ?: reading?.takeIf { available && it.name != null }?.let { lookup(it) }
        val relationName = outcome.relationName ?: lookup?.let { reading!!.name }
        val captors = if (outcome.status == Status.RESOLVED && anchor != null) possibleCaptors(anchor, ownerOfAnchor()) else outcome.sourceCandidates
        return Outcome(
            Status.CAPTURE_NOT_EXCLUDED, anchor, relationName, lookup,
            captors + conditional.flatMap { viewOf(it) }, outcome.externalCandidates
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

    /** Each followed value with the anchor that led to it (the request for the first step, else the previous step's reference). */
    private fun followedAnchors(): List<Pair<WrittenName, ProjectDeclarationView>> {
        val result = ArrayList<Pair<WrittenName, ProjectDeclarationView>>()
        for ((i, step) in source.steps.withIndex()) {
            if (step !is ProjectPackageValueView) continue
            val anchor = if (i == 0) startAnchor() else anchorOf(source.steps[i - 1])
            if (anchor != null && anchor.kind == ProjectTypeReference.Anchor.TYPE) result += anchor to step
        }
        return result
    }

    /** Found relation readings of a followed anchor with at least two segments (a one-segment anchor binds in package scope). */
    private fun competing(name: ProjectQualifiedName): List<ProjectExternalLookup> {
        val segments = name.segments
        if (segments.size < 2) return emptyList()
        val found = ArrayList<ProjectExternalLookup>()
        if (currentSchema != null) found += metadata.findRelation(currentSchema, segments[0])
        if (segments.size >= 3) found += metadata.findRelation(segments[0], segments[1])
        return found.filter { it.status == ProjectExternalLookup.Status.FOUND }
    }

    private fun startAnchor(): WrittenName? = when {
        written != null -> WrittenName(written.name, written.anchor, null)
        startReference?.kind == ProjectTypeReference.Kind.ANCHORED -> WrittenName(startReference.name, startReference.anchor.get(), startReference)
        else -> null
    }

    private fun anchorOf(step: ProjectDeclarationView): WrittenName? {
        val reference = when (step) {
            is ProjectPackageValueView -> step.typeRef
            is ProjectSubtypeView -> step.baseType
            else -> null
        } ?: return null
        return if (reference.kind == ProjectTypeReference.Kind.ANCHORED) WrittenName(reference.name, reference.anchor.get(), reference) else null
    }

    /** The anchor the walk stopped at: the terminal reference, else the written request. */
    private fun stoppedAnchor(): WrittenName? {
        if (source.status != ProjectTypeDescription.Status.ANCHORED_REFERENCE && source.status != ProjectTypeDescription.Status.UNRESOLVED_VALUE) return null
        val terminal = source.terminalReference.orElse(null)
        if (terminal != null) return if (terminal.kind == ProjectTypeReference.Kind.ANCHORED) WrittenName(terminal.name, terminal.anchor.get(), terminal) else null
        return written?.let { WrittenName(it.name, it.anchor, null) }
    }

    private fun ownerOfAnchor(): QualifiedName? = if (trace.written) packageOwner?.name else trace.holder?.owner

    private fun reading(anchor: WrittenName): Reading? {
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
