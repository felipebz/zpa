//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Classification retained by the existing extractor. It carries no element, attribute or
 * cursor-return details.
 */
@ZpaExperimentalApi
enum class ProjectTypeShape {
    RECORD,
    OBJECT,
    COLLECTION,
    REF_CURSOR
}
