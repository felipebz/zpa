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
import com.felipebz.zpa.project.ProjectIndexPreparationResult
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
