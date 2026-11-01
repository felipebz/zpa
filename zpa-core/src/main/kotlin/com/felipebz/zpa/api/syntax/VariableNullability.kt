//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** The nullability clause written on a variable declaration. */
@ZpaExperimentalApi
public enum class VariableNullability {
    UNSPECIFIED,
    NULLABLE,
    NOT_NULL,
}
