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
