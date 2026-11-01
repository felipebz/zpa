//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Specification/body/standalone role retained by the declaration fact. */
@ZpaExperimentalApi
enum class ProjectDeclarationRole {
    SPECIFICATION,
    BODY,
    STANDALONE
}
