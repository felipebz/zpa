//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    fileIds: List<String>,
    // Plain identity token for declaration IDs; it holds no reference back to this snapshot.
    private val scope: ProjectSnapshotScope = ProjectSnapshotScope.issue()
) {
    private val failureList = failures
    private val fileIdList = fileIds

    /**
     * This snapshot as seen from a unit compiling in [schema]: every lookup of the returned snapshot reads an
     * unqualified name in that schema first, as Oracle and the analysis do. Among the declarations a lookup matches,
     * those whose effective schema (written owner, else the file's mapped schema, else the default compilation schema)
     * is [schema] are the only candidates when there are some; otherwise every match stays one, as a synonym may lead
     * there. Pass the schema of the unit the name is written in: `ProjectExternalContext.compilationSchema(unitName,
     * fileId)`. The returned snapshot issues the same declaration IDs; a null [schema] returns this snapshot.
     */
    fun preferring(schema: ProjectIdentifier?): ProjectSnapshot {
        if (schema == null) return this
        val index = preparationResult.index.preferring(schema.identifier)
        if (index === preparationResult.index) return this
        return ProjectSnapshot(preparationResult.withIndex(index), preparationState, failureList, fileIdList, scope)
    }

    /**
     * The names of the standalone procedures and functions whose heading was identified in a project file but produced
     * no declaration (for example a heading the extractor could not read completely). They are never call candidates,
     * but they show that the name exists: a client tracking which files use a name must treat their appearance or
     * disappearance as a change of that name. Empty for an unprepared snapshot.
     */
    val omittedStandaloneCallables: Set<ProjectQualifiedName>
        get() = Collections.unmodifiableSet(preparationResult.index.omittedStandaloneCallables.mapTo(LinkedHashSet()) { ProjectQualifiedName.wrap(it) })

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
     * Whether an `ALTER TYPE` of the project adds, drops or modifies an attribute of a type named [name], matched by its
     * last segment as the analysis does (the schema of an unqualified name is unknown). The added attributes are not
     * extracted: the type's [ProjectStandaloneTypeView.attributes] are those of its `CREATE TYPE`.
     */
    fun isAttributeListAltered(name: ProjectQualifiedName): Boolean =
        preparationState != ProjectPreparationState.NOT_PREPARED && preparationResult.index.alteredTypes.attributesOf(name.name)

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
     * A qualified [name] overrides [packageOwner]. A one-segment name resolves to the package's own type; when the
     * package has none, it reaches standalone types only if the project holds exactly one specification of the package
     * and its member names prove the name undeclared, as the analysis does. A candidate's
     * [ProjectDeclarationView.kind] tells a package type from a standalone one.
     */
    fun resolveNamedType(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?): ProjectNamedTypeResolution {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectNamedTypeResolution.notPrepared(name, packageOwner)
        }
        val occurrences = ProjectTypeNameLookup.typeOccurrences(preparationResult.index, name.name, packageOwner?.name)
        val candidates = ProjectSnapshotDeclarations.views(scope, occurrences).map { it as ProjectTypeDeclarationView }
        return ProjectNamedTypeResolution.of(name, packageOwner, candidates, failures)
    }

    /**
     * Bounded description of a type [name]: follows package subtype aliases only and classifies recognized built-in
     * names, using the lookup rules of [resolveNamedType]. [packageOwner] has the same meaning as there (one-segment
     * names use it, reaching standalone types only where the package proves the name undeclared, or the standalone
     * namespace when null; qualified names use their own prefix; no schema inference); pass the owner of the declaration
     * that contains the name, not a guess from file paths. A name has no source range, so
     * [ProjectTypeDescription.terminalReference] is empty when the query stops at the initial name.
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
        ProjectExternalTypeDescriber.describe(preferring(currentSchema), name, null, packageOwner, null, metadata, currentSchema)

    // Every external description is made from the unit compiling in [currentSchema]: its unqualified names prefer the
    // declarations of that schema ([preferring]).

    /** As the name overload, for a retained [reference] (see [describeType] for the reference overload). */
    fun describeExternalType(
        reference: ProjectTypeReference,
        packageOwner: ProjectQualifiedName?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectExternalTypeDescription =
        ProjectExternalTypeDescriber.describe(preferring(currentSchema), reference.name, reference, packageOwner, null, metadata, currentSchema)

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
        ProjectExternalTypeDescriber.describe(preferring(currentSchema), anchor.name, null, packageOwner, anchor, metadata, currentSchema)

    /**
     * Classified lookup of one package variable or constant by its written [name]. A one-segment name uses the
     * explicit [packageOwner] (with a null owner there is no value namespace to search: nothing is inferred and no
     * package is scanned); a qualified name uses its own prefix and ignores [packageOwner] for candidate selection
     * (the supplied owner is still reported in the result). [ProjectValueResolution.candidates] holds every known
     * declaration with the effective qualified name, of any kind, in index order; only a package value is an eligible
     * target (a package exception or cursor of that name is a candidate, and the status stays `NOT_FOUND_IN_PROJECT`:
     * read the candidate to use it). An unprepared snapshot gives `NOT_PREPARED`, then any preparation failure gives `INCOMPLETE_INDEX`
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

    /**
     * Narrows a [structural] resolution issued by this snapshot with per-actual datatype evidence: [argumentTypes] has
     * one element per written actual (`structural.arguments.arguments`, same order). Each applicable candidate's own
     * bindings are compared with its formals, described with [describeExternalType] under [metadata] (which may be
     * [ProjectExternalMetadata.none]) and [currentSchema]; a candidate is removed only when a binding is proven
     * incompatible on Oracle. The structural result, its candidates and bindings are reused, never recomputed; nothing
     * is ranked or chosen by order, and default expressions are not evaluated. Without evidence the structural
     * RESOLVED/AMBIGUOUS outcome is kept.
     *
     * @throws IllegalArgumentException if a candidate was issued by another snapshot, if the list size differs from the
     * number of written actuals, or if it contains null
     */
    fun narrowCall(
        structural: ProjectCallResolution,
        argumentTypes: List<ProjectArgumentType>,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectTypedCallResolution = ProjectCallNarrower.narrow(preferring(currentSchema), structural, argumentTypes, metadata, currentSchema)

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

    /**
     * The written type of the member [name] of a value of the type [type] declares: one record field, or one attribute
     * of an object type, inherited ones included, as [ProjectRecordMemberResolver] finds it, with the package its
     * field is looked up from. Null when the member is not one field or attribute (a method, a collection method, no such
     * member, several, or an attribute list not fully known), or [type] is no such type of this snapshot.
     */
    @JvmSynthetic
    internal fun memberType(type: ProjectDeclarationId, name: OracleIdentifier): ProjectMemberType? {
        val resolved = memberResolution(type, name) as? ProjectRecordMemberResolution.Resolved ?: return null
        val owner = (resolved.declaration as? PackageTypeDeclaration)?.owner?.let(ProjectQualifiedName::wrap)
        return ProjectMemberType(ProjectTypeReference.from(resolved.field.typeRef), owner)
    }

    /**
     * What the member [name] of a value of the type [type] declares is, as [ProjectRecordMemberResolver] finds it: one
     * field or attribute (with the type declaring it), a method set, a collection method, or why there is none. Null
     * without a prepared project, or when [type] is no type of this snapshot.
     */
    @JvmSynthetic
    internal fun memberResolution(type: ProjectDeclarationId, name: OracleIdentifier): ProjectRecordMemberResolution? {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) return null
        val declaration = occurrenceOf(type)?.declaration as? ProjectTypeDeclaration ?: return null
        // Built per call: the snapshot retains only durable facts.
        return ProjectRecordMemberResolver(callContext()).resolve(declaration, name)
    }

    /**
     * The result of calling the method [name], with [arguments], on a value of the object type [type]: the RETURN type
     * of the one method the call binds ([ProjectMethodCallBinder]), or the type itself for a function returning `SELF`
     * as result. Null when the call binds no single method (ambiguous, several applicable, none, a procedure, an
     * incomplete set), or [name] names no method.
     */
    @JvmSynthetic
    internal fun methodResultType(type: ProjectDeclarationId, name: OracleIdentifier, arguments: com.felipebz.zpa.project.ProjectCallArguments): ProjectMemberType? {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) return null
        val declaration = occurrenceOf(type)?.declaration as? ProjectTypeDeclaration ?: return null
        val members = ProjectRecordMemberResolver(callContext())
        val methods = members.resolve(declaration, name) as? ProjectRecordMemberResolution.Method ?: return null
        val call = ProjectMethodCallBinder(members).bind(methods, ProjectMethodReceiver.VALUE, InternalCallKind.FUNCTION, arguments)
        if (call.status != ProjectMethodCall.Status.RESOLVED) return null
        val selected = call.selected ?: return null
        val method = selected.method
        if (method == null || method.returnsSelf) return ProjectMemberType(null, null, ProjectQualifiedName.wrap(selected.type.name))
        return method.returnType?.let { ProjectMemberType(ProjectTypeReference.from(it), null) }
    }

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

/**
 * The written type of a record field, an object attribute or a method's result, and the package it is looked up from
 * (null for a schema type); or, for a method returning its own type, that type's name.
 */
@OptIn(ZpaExperimentalApi::class)
internal class ProjectMemberType(val typeRef: ProjectTypeReference?, val owner: ProjectQualifiedName?, val typeName: ProjectQualifiedName? = null)
