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
import com.felipebz.zpa.project.*
import java.util.*
import com.felipebz.zpa.project.ProjectCallKind as InternalCallKind

/**
 * Immutable metadata facade retaining the existing authoritative preparation result/index.
 * Safe to retain after preparation: no input, reader, full source text, extractor, parser,
 * AST or file-local semantic state is reachable through this snapshot. Public lists are
 * runtime-unmodifiable, including from Java, and metadata reads never reacquire sources.
 */
@ZpaExperimentalApi
class ProjectSnapshot private constructor(
    private val preparationResult: ProjectIndexPreparationResult,
    val preparationState: ProjectPreparationState,
    // Both metadata lists are privately owned by the preparation adapter; wrap without copying.
    failures: List<ProjectPreparationFailure>,
    fileIds: List<String>
) {
    // Plain identity token for declaration IDs; it holds no reference back to this snapshot.
    private val scope = ProjectSnapshotScope.issue()

    val attemptedFileCount: Int get() = preparationResult.attemptedFileCount

    /** Sources whose read/extraction completed, not a syntactic or coverage guarantee. */
    val successfulFileCount: Int get() = preparationResult.successfulFileCount

    /** Runtime-unmodifiable failure summaries in deterministic file-identity order. */
    val failures: List<ProjectPreparationFailure> = Collections.unmodifiableList(failures)

    /**
     * All attempted source identities, including failed sources, in existing index order.
     * IDs are opaque; ordering is deterministic presentation order, not semantic ranking.
     */
    val fileIds: List<String> = Collections.unmodifiableList(fileIds)

    /**
     * All known declaration occurrences in deterministic index order (file ID, then source
     * position), including duplicates and without semantic ranking. Known facts are returned
     * even when preparation had failures; consult [preparationState]. Each call
     * creates fresh views; compare them through [ProjectDeclarationView.id].
     */
    val declarations: List<ProjectDeclarationView>
        get() {
            if (preparationState == ProjectPreparationState.NOT_PREPARED) {
                return Collections.emptyList()
            }
            return ProjectSnapshotDeclarations.views(scope, preparationResult.index.occurrences)
        }

    /**
     * Known declarations of exactly the opaque file ID, in per-file order. An ID that names no
     * prepared file (including blank IDs) yields an empty inventory, never another namespace's
     * declarations. Status reflects project-wide preparation failures, not only this file's.
     */
    fun declarationsFor(fileId: String): ProjectInventory<ProjectDeclarationView> =
        inventory(ProjectSnapshotDeclarations.occurrencesFor(preparationResult.index, fileId))

    /**
     * Exact structured-name matches using existing index name semantics. This is inventory,
     * not resolution: duplicates are kept, nothing is ranked, no scope/schema is inferred.
     */
    fun findDeclarations(name: ProjectQualifiedName): ProjectInventory<ProjectDeclarationView> =
        inventory(preparationResult.index.findOccurrences(name.name))

    /**
     * Resolves a structured type name using the existing project rules, without package context.
     * One-segment names use the standalone namespace; qualified names use their own prefix.
     * Not a datatype resolver: builtins, subtype bases, anchors and `REF` are not interpreted.
     */
    fun resolveNamedType(name: ProjectQualifiedName): ProjectNamedTypeResolution = resolveNamedType(name, null)

    /**
     * As [resolveNamedType], with explicit package context used only for one-segment names.
     * A qualified [name] overrides [packageOwner]; there is no fallback to standalone types.
     */
    fun resolveNamedType(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?): ProjectNamedTypeResolution {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectNamedTypeResolution.notPrepared(name, packageOwner)
        }
        val occurrences = preparationResult.index.findTypeOccurrences(
            ProjectTypeNameLookup.owner(name.name, packageOwner?.name),
            name.name.last
        )
        val candidates = ProjectSnapshotDeclarations.views(scope, occurrences).map { it as ProjectTypeDeclarationView }
        return ProjectNamedTypeResolution.of(name, packageOwner, candidates, failures)
    }

    /**
     * Bounded description of a type [name]: follows package subtype aliases only and classifies recognized built-in
     * names, using the lookup rules of [resolveNamedType]. [packageOwner] has the same meaning as there (one-segment
     * names use it, or the standalone namespace when null; qualified names use their own prefix; no schema inference,
     * no fallback); pass the owner of the declaration that contains the name, not a guess from file paths. A name has
     * no source range, so [ProjectTypeDescription.terminalReference] is empty when the query stops at the initial name.
     */
    fun describeType(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?): ProjectTypeDescription =
        ProjectTypeDescriber.describe(this, name, null, packageOwner)

    /**
     * As the name overload for a retained [reference] (a field, element, subtype base, return or value type), keeping
     * the distinction between named, anchored and `REF` references. Supported source-backed `%TYPE` anchors are
     * followed through package values; other anchors stop with [ProjectTypeDescription.Status.ANCHORED_REFERENCE]
     * (including every `%ROWTYPE`) or [ProjectTypeDescription.Status.UNRESOLVED_VALUE], and `REF` stops with
     * [ProjectTypeDescription.Status.REF_REFERENCE], even when the name matches a project type. [packageOwner] should
     * be the owner of the declaration holding the reference.
     */
    fun describeType(reference: ProjectTypeReference, packageOwner: ProjectQualifiedName?): ProjectTypeDescription =
        ProjectTypeDescriber.describe(this, reference.name, reference, packageOwner)

    /**
     * Describes a caller-written `%TYPE`/`%ROWTYPE` [anchor] (one not retained by the index: an anonymous block, a
     * package body, a local declaration) with the same walk as [describeType] for retained references.
     *
     * Precondition, owned by the caller's lexical analysis: the first segment is not bound by a declaration lexically
     * visible at the reference that this snapshot does not index (locals, parameters, body-private declarations).
     * [packageOwner] is the package whose specification or body contains the reference, or null. A reference that is
     * itself an indexed specification fact should be described through its retained [ProjectTypeReference] instead.
     *
     * Every result echoes the request in [ProjectTypeDescription.writtenAnchor]. When the walk stops at the written
     * anchor itself, [ProjectTypeDescription.terminalReference] is empty (no source range is invented); after a value
     * was followed, it is the genuine retained reference. `%ROWTYPE` always stops at the request, also without owner; a
     * one-segment `%TYPE` without owner stops with `ENCLOSING_SCOPE`, as there is no package namespace to search.
     */
    fun describeType(anchor: ProjectWrittenAnchor, packageOwner: ProjectQualifiedName?): ProjectTypeDescription =
        ProjectTypeDescriber.describe(this, anchor.name, null, packageOwner, anchor)

    /**
     * Composes [describeType] for [name] with supplied external relation [metadata]. [currentSchema] is the schema in
     * which the referencing unit compiles; it is needed only for unqualified relation readings (null gives
     * [ProjectExternalTypeDescription.Status.SCHEMA_CONTEXT_MISSING] there). The source description is unchanged and
     * returned in [ProjectExternalTypeDescription.source].
     */
    fun describeExternalType(
        name: ProjectQualifiedName,
        packageOwner: ProjectQualifiedName?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectExternalTypeDescription =
        ProjectExternalTypeDescriber.describe(this, name, null, packageOwner, null, metadata, currentSchema)

    /** As the name overload, for a retained [reference] (see [describeType] for the reference overload). */
    fun describeExternalType(
        reference: ProjectTypeReference,
        packageOwner: ProjectQualifiedName?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectExternalTypeDescription =
        ProjectExternalTypeDescriber.describe(this, reference.name, reference, packageOwner, null, metadata, currentSchema)

    /**
     * As the name overload, for a caller-written [anchor], with the same lexical precondition as the written
     * [describeType] overload: the caller has excluded local, parameter and body-private captures.
     */
    fun describeExternalType(
        anchor: ProjectWrittenAnchor,
        packageOwner: ProjectQualifiedName?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectExternalTypeDescription =
        ProjectExternalTypeDescriber.describe(this, anchor.name, null, packageOwner, anchor, metadata, currentSchema)

    /**
     * Classified lookup of one package variable or constant by its written [name]. A one-segment name uses the
     * explicit [packageOwner] (with a null owner there is no value namespace to search: nothing is inferred and no
     * package is scanned); a qualified name uses its own prefix and ignores [packageOwner] for candidate selection
     * (the supplied owner is still reported in the result). [ProjectValueResolution.candidates] holds every known
     * declaration with the effective qualified name, of any kind, in index order; only a package value is an eligible
     * target. An unprepared snapshot gives `NOT_PREPARED`, then any preparation failure gives `INCOMPLETE_INDEX`
     * (even without candidates or owner); `NOT_FOUND_IN_PROJECT` is not proof about Oracle. No scope rules, field
     * paths or `%TYPE` interpretation are applied.
     */
    fun resolvePackageValue(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?): ProjectValueResolution {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectValueResolution.notPrepared(name, packageOwner)
        }
        val effective = if (name.name.segments.size == 1) packageOwner?.name?.append(name.name.last) else name.name
        val occurrences = effective?.let { preparationResult.index.findOccurrences(it) }.orEmpty()
        return ProjectValueResolution.of(name, packageOwner, ProjectSnapshotDeclarations.views(scope, occurrences), failures)
    }

    /**
     * Exact inventory of package procedures and functions declared with exactly this package
     * owner and local name: specifications and bodies, overloads and duplicates, in index order.
     * This is not call resolution: nothing is filtered, paired, deduplicated or ranked, and a
     * body without a specification is not thereby private or public. An empty inventory is not
     * proof that the callable is absent from Oracle.
     */
    fun findPackageSubprograms(
        packageOwner: ProjectQualifiedName,
        name: ProjectIdentifier
    ): ProjectInventory<ProjectPackageSubprogramView> {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectInventory.notPrepared()
        }
        val occurrences = preparationResult.index.findSubprogramOccurrences(packageOwner.name, name.identifier)
        val candidates = ProjectSnapshotDeclarations.views(scope, occurrences).map { it as ProjectPackageSubprogramView }
        return ProjectInventory.of(candidates, failures)
    }

    /**
     * Call candidate discovery for [target] over this snapshot's prepared index, using the project's existing
     * namespace, coverage and precedence rules. Only package SPECIFICATION and STANDALONE procedures/functions are
     * candidates; bodies never are, and a standalone heading whose signature was not retained is evidence only. This
     * is not call resolution: no argument is bound and nothing is selected. No schema, lexical scope or catalog is
     * inferred, and no source is read again.
     */
    fun findCallCandidates(target: ProjectCallTarget): ProjectCallCandidates {
        val result = ProjectCallCandidateFinder(callContext()).find(target.toInternal())
        return ProjectCallCandidates.of(result, target, scope, failures)
    }

    /**
     * Structural resolution of a call to [target] with [arguments] in a [kind] context: [findCallCandidates], then
     * kind and positional/named/mixed association against each candidate's retained formals, with omitted defaults.
     * An invalid argument notation is reported before anything else. Datatypes and actual expressions are never
     * considered, and nothing is chosen by position: one APPLICABLE candidate of the requested kind under a certain
     * target is [ProjectCallResolution.Status.RESOLVED], several are AMBIGUOUS.
     */
    fun resolveCall(target: ProjectCallTarget, kind: ProjectCallKind, arguments: ProjectCallArguments): ProjectCallResolution {
        val internalKind = when (kind) {
            ProjectCallKind.PROCEDURE -> InternalCallKind.PROCEDURE
            ProjectCallKind.FUNCTION -> InternalCallKind.FUNCTION
        }
        val result = ProjectCallBinder(callContext()).resolve(target.toInternal(), internalKind, arguments.internal)
        return ProjectCallResolution.of(result, target, kind, arguments, scope, failures)
    }

    // An unprepared snapshot holds a placeholder empty result; it must stay NOT_PREPARED, not become "prepared".
    private fun callContext(): ProjectAnalysisContext =
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            ProjectAnalysisContext.NOT_PREPARED
        } else {
            ProjectAnalysisContext.prepared(preparationResult)
        }

    /**
     * Correlates the package-body procedure/function occurrence [bodyId] with specification
     * occurrences of this same snapshot, using the existing exact heading matching.
     *
     * @throws NullPointerException if [bodyId] is null
     * @throws IllegalArgumentException if the ID was issued by another snapshot, names no
     *   occurrence, or does not identify a package procedure/function with role BODY
     */
    fun correlateSpecification(bodyId: ProjectDeclarationId): ProjectSpecificationCorrelation {
        require(bodyId.isIssuedBy(scope)) { "Declaration ID was not issued by this snapshot" }
        val occurrence = ProjectSnapshotDeclarations.occurrencesFor(preparationResult.index, bodyId.fileId)
            .getOrNull(bodyId.ordinal)
        requireNotNull(occurrence) { "Declaration ID names no indexed occurrence" }
        val fact = occurrence.declaration
        require(fact is PackageSubprogramDeclaration && fact.role == DeclarationRole.BODY) {
            "Specification correlation requires a package procedure/function BODY declaration"
        }
        val body = ProjectSnapshotDeclarations.views(scope, listOf(occurrence)).single() as ProjectPackageSubprogramView
        val candidates = ProjectSnapshotDeclarations
            .views(scope, preparationResult.index.findSpecificationOccurrences(fact))
            .map { it as ProjectPackageSubprogramView }
        return ProjectSpecificationCorrelation.of(body, candidates, failures)
    }

    /** The authoritative index, for the internal description walk only. */
    @get:JvmSynthetic
    internal val projectIndex: ProjectSymbolIndex get() = preparationResult.index

    /** The canonical occurrence behind an ID issued by this snapshot, or null. */
    @JvmSynthetic
    internal fun occurrenceOf(id: ProjectDeclarationId): IndexedDeclaration? =
        if (!id.isIssuedBy(scope)) null
        else ProjectSnapshotDeclarations.occurrencesFor(preparationResult.index, id.fileId).getOrNull(id.ordinal)

    /** Views of indexed occurrences, issued by this snapshot. */
    @JvmSynthetic
    internal fun viewsOf(occurrences: List<IndexedDeclaration>): List<ProjectDeclarationView> =
        ProjectSnapshotDeclarations.views(scope, occurrences)

    private fun inventory(occurrences: List<IndexedDeclaration>): ProjectInventory<ProjectDeclarationView> {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectInventory.notPrepared()
        }
        return ProjectInventory.of(ProjectSnapshotDeclarations.views(scope, occurrences), failures)
    }

    internal companion object {
        /**
         * Builds the public snapshot over an already prepared (or unprepared) analysis context.
         * Nothing is read or extracted: the authoritative result and index are reused directly.
         * Each call issues a new identity scope, so callers must create it once per scan.
         */
        @JvmSynthetic
        fun create(context: ProjectAnalysisContext): ProjectSnapshot = when (val state = context.state) {
            ProjectAnalysisContext.State.NotPrepared -> create(
                ProjectIndexPreparationResult(ProjectSymbolIndex.empty(), 0, emptyList()),
                ProjectPreparationState.NOT_PREPARED, emptyList(), emptyList()
            )
            is ProjectAnalysisContext.State.Prepared -> create(
                state.result,
                when (state.kind) {
                    ProjectAnalysisContext.State.Kind.NOT_PREPARED -> ProjectPreparationState.NOT_PREPARED
                    ProjectAnalysisContext.State.Kind.PREPARED_EMPTY -> ProjectPreparationState.PREPARED_EMPTY
                    ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS ->
                        ProjectPreparationState.PREPARED_WITH_DECLARATIONS
                    ProjectAnalysisContext.State.Kind.PREPARED_WITH_FAILURES ->
                        ProjectPreparationState.PREPARED_WITH_FAILURES
                },
                state.result.failures.map { ProjectPreparationFailure.create(it.fileId.value, it.exceptionType) },
                state.result.index.fileIds.map { it.value }
            )
        }

        @JvmSynthetic
        fun create(
            preparationResult: ProjectIndexPreparationResult,
            preparationState: ProjectPreparationState,
            failures: List<ProjectPreparationFailure>,
            fileIds: List<String>
        ) = ProjectSnapshot(preparationResult, preparationState, failures, fileIds)
    }
}
