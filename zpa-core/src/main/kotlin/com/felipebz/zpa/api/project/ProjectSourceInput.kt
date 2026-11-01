//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
