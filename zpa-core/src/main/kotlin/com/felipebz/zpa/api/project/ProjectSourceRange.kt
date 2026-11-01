//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * Token-compatible source range: lines are one-based, columns are zero-based and the end
 * position is exclusive. The file ID is opaque; no filesystem meaning is implied.
 */
@ZpaExperimentalApi
class ProjectSourceRange private constructor(
    val fileId: String,
    /** One-based. */
    val startLine: Int,
    /** Zero-based. */
    val startColumn: Int,
    /** One-based. */
    val endLine: Int,
    /** Zero-based, exclusive. */
    val endColumn: Int
) {
    override fun equals(other: Any?): Boolean =
        other is ProjectSourceRange && startLine == other.startLine && startColumn == other.startColumn &&
            endLine == other.endLine && endColumn == other.endColumn && fileId == other.fileId

    override fun hashCode(): Int = Objects.hash(fileId, startLine, startColumn, endLine, endColumn)

    override fun toString(): String = "$fileId:$startLine:$startColumn-$endLine:$endColumn"

    internal companion object {
        @JvmSynthetic
        fun create(fileId: String, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) =
            ProjectSourceRange(fileId, startLine, startColumn, endLine, endColumn)
    }
}
