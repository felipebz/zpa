//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Kind of package subprogram represented by a public project-analysis view. */
@ZpaExperimentalApi
enum class PackageSubprogramKind {
    PROCEDURE,
    FUNCTION
}
