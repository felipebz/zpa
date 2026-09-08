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
package com.felipebz.zpa.project

import java.util.Collections

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

/** Copies a collection into a list whose mutation methods are disabled. */
internal fun <T> immutableList(values: Collection<T>): List<T> =
    Collections.unmodifiableList(values.toList())
