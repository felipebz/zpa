//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

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

    @JvmSynthetic
    internal fun isIssuedBy(snapshotScope: ProjectSnapshotScope): Boolean = scope === snapshotScope

    override fun toString(): String = "$fileId#$ordinal"

    internal companion object {
        @JvmSynthetic
        fun create(scope: ProjectSnapshotScope, fileId: String, ordinal: Int) =
            ProjectDeclarationId(scope, fileId, ordinal)
    }
}
