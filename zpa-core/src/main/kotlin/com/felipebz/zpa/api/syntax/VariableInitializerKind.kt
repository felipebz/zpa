//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** The syntax form used to initialize a variable declaration. */
@ZpaExperimentalApi
public enum class VariableInitializerKind {
    ASSIGNMENT,
    DEFAULT,
}
