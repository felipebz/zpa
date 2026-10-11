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
import com.felipebz.zpa.project.IndexedDeclaration
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.findSpecificationOccurrences
import com.felipebz.zpa.project.ProjectTypeNameLookup
import java.util.Collections

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
    private val scope = ProjectSnapshotScope()

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

    private fun inventory(occurrences: List<IndexedDeclaration>): ProjectInventory<ProjectDeclarationView> {
        if (preparationState == ProjectPreparationState.NOT_PREPARED) {
            return ProjectInventory.notPrepared()
        }
        return ProjectInventory.of(ProjectSnapshotDeclarations.views(scope, occurrences), failures)
    }

    internal companion object {
        @JvmSynthetic
        fun create(
            preparationResult: ProjectIndexPreparationResult,
            preparationState: ProjectPreparationState,
            failures: List<ProjectPreparationFailure>,
            fileIds: List<String>
        ) = ProjectSnapshot(preparationResult, preparationState, failures, fileIds)
    }
}
