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
import java.util.Objects

/**
 * Identifies one indexed declaration occurrence within the snapshot that issued it. Equality
 * covers a private per-snapshot scope plus the (file ID, ordinal) coordinate, so IDs from
 * different snapshots are never equal. The coordinate is deterministic inventory metadata,
 * not the complete identity. No persistence or cross-snapshot guarantee is made.
 * Holding an ID does not retain the snapshot or its index.
 */
@ZpaExperimentalApi
class ProjectDeclarationId private constructor(
    private val scope: ProjectSnapshotScope,
    val fileId: String,
    /** Zero-based position within that file's frozen declaration sequence. */
    val ordinal: Int
) {
    override fun equals(other: Any?): Boolean =
        other is ProjectDeclarationId && scope === other.scope && ordinal == other.ordinal && fileId == other.fileId

    override fun hashCode(): Int = Objects.hash(System.identityHashCode(scope), fileId, ordinal)

    override fun toString(): String = "$fileId#$ordinal"

    internal companion object {
        @JvmSynthetic
        fun create(scope: ProjectSnapshotScope, fileId: String, ordinal: Int) =
            ProjectDeclarationId(scope, fileId, ordinal)
    }
}
