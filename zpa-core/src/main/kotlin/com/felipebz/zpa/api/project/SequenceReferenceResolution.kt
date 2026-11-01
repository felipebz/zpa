//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Conservative result of resolving a sequence pseudocolumn reference. */
@ZpaExperimentalApi
enum class SequenceReferenceResolution {
    RESOLVED_SEQUENCE,
    RESOLVED_NON_SEQUENCE,
    UNKNOWN
}
