//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/** Stable source identity retained by project-level semantic data. */
@JvmInline
value class FileId(val value: String) {
    init {
        require(value.isNotBlank()) { "A file identity cannot be blank" }
    }

    override fun toString() = value
}

/**
 * A token-compatible source range. Lines are one-based, columns are zero-based, and the
 * end position is exclusive, matching FLR token positions.
 */
data class SourceRange(
    val fileId: FileId,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
    init {
        require(startLine > 0) { "Source lines are one-based" }
        require(endLine > 0) { "Source lines are one-based" }
        require(startColumn >= 0) { "Source columns are zero-based" }
        require(endColumn >= 0) { "Source columns are zero-based" }
    }
}

/**
 * Copies a collection into a list of its exact size whose mutation methods throw, with no wrapper around the copy:
 * the shared empty list when empty, no array for one or two elements, and an already copied list is returned as is.
 * The elements must not be null.
 */
internal fun <T : Any> immutableList(values: Collection<T>): List<T> =
    java.util.List.copyOf(values)
