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
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.IndexedDeclaration
import com.felipebz.zpa.project.ProjectDeclaration
import com.felipebz.zpa.project.ProjectSymbolIndex
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.qualifiedName
import java.util.Collections
import com.felipebz.zpa.project.ProjectDeclarationKind as FactKind

/** Projects public declaration views from canonical index occurrences. */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectSnapshotDeclarations {
    @JvmSynthetic
    fun views(scope: ProjectSnapshotScope, occurrences: List<IndexedDeclaration>): List<ProjectDeclarationView> =
        Collections.unmodifiableList(occurrences.map { view(scope, it) })

    @JvmSynthetic
    fun occurrencesFor(index: ProjectSymbolIndex, fileId: String): List<IndexedDeclaration> =
        // Blank IDs cannot name a file (they are rejected at preparation), so nothing matches.
        if (fileId.isBlank()) emptyList() else index.occurrencesFor(FileId(fileId))

    private fun view(scope: ProjectSnapshotScope, occurrence: IndexedDeclaration): ProjectDeclarationView =
        BackedDeclarationView(
            ProjectDeclarationId.create(scope, occurrence.fileId.value, occurrence.ordinal),
            occurrence.declaration
        )

    /** Retains only the issued ID and the authoritative fact; never the snapshot or index. */
    private class BackedDeclarationView(
        override val id: ProjectDeclarationId,
        private val fact: ProjectDeclaration
    ) : ProjectDeclarationView {
                override val kind: ProjectDeclarationKind
            get() = when (fact.kind) {
            FactKind.PACKAGE -> ProjectDeclarationKind.PACKAGE
            FactKind.PACKAGE_BODY -> ProjectDeclarationKind.PACKAGE_BODY
            FactKind.STANDALONE_TYPE -> ProjectDeclarationKind.STANDALONE_TYPE
            FactKind.PACKAGE_TYPE -> ProjectDeclarationKind.PACKAGE_TYPE
            FactKind.PACKAGE_SUBTYPE -> ProjectDeclarationKind.PACKAGE_SUBTYPE
            FactKind.PACKAGE_PROCEDURE -> ProjectDeclarationKind.PACKAGE_PROCEDURE
            FactKind.PACKAGE_FUNCTION -> ProjectDeclarationKind.PACKAGE_FUNCTION
            FactKind.SEQUENCE -> ProjectDeclarationKind.SEQUENCE
        }

        override val role: ProjectDeclarationRole
            get() = when (fact.role) {
            DeclarationRole.SPECIFICATION -> ProjectDeclarationRole.SPECIFICATION
            DeclarationRole.BODY -> ProjectDeclarationRole.BODY
            DeclarationRole.STANDALONE -> ProjectDeclarationRole.STANDALONE
        }

        override val qualifiedName: ProjectQualifiedName
            get() = ProjectQualifiedName.wrap(fact.qualifiedName())

        override val fileId: String get() = fact.fileId.value

        override val sourceRange: ProjectSourceRange get() = fact.sourceRange.toPublic()

        override fun toString() = "$kind $qualifiedName [$id]"
    }

    private fun SourceRange.toPublic() = ProjectSourceRange.create(fileId.value, startLine, startColumn, endLine, endColumn)
}
