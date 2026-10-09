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

/**
 * Transient preparation input, not a durable project fact. It may retain source text or
 * a reader and its captures; the returned snapshot retains none of them. File identities
 * are opaque strings, validated as nonblank and unique by preparation before any read.
 */
@ZpaExperimentalApi
class ProjectSourceInput private constructor(
    /** The opaque caller-supplied identity, not a schema or filesystem location. */
    val fileId: String,
    private val reader: ProjectSourceReader
) {
    @JvmSynthetic
    internal fun read(): String = reader.read()

    companion object {
        /** Creates an input from caller-supplied, already decoded text. */
        @JvmStatic
        fun ofText(fileId: String, contents: String): ProjectSourceInput =
            ProjectSourceInput(fileId) { contents }

        /** Creates an input whose reader runs during each preparation, without memoization. */
        @JvmStatic
        fun ofReader(fileId: String, reader: ProjectSourceReader): ProjectSourceInput =
            ProjectSourceInput(fileId, reader)
    }
}
