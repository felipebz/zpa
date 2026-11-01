//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Durable failure summary: no exception, message, cause, stack trace or parser state. */
@ZpaExperimentalApi
class ProjectPreparationFailure private constructor(
    val fileId: String,
    /** Fully qualified class name of the caught Exception, not its root cause. */
    val exceptionType: String
) {
    internal companion object {
        @JvmSynthetic
        fun create(fileId: String, exceptionType: String) = ProjectPreparationFailure(fileId, exceptionType)
    }
}
