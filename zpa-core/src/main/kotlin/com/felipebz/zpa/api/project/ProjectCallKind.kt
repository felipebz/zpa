//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Syntactic context of a call: a statement calls a procedure, an expression calls a function. */
@ZpaExperimentalApi
enum class ProjectCallKind {
    PROCEDURE,
    FUNCTION
}
