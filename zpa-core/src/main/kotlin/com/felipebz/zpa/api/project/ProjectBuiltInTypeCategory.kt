//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Broad category of a built-in type recognized by name. It carries no length, precision, scale or nullability. */
@ZpaExperimentalApi
enum class ProjectBuiltInTypeCategory {
    NUMERIC,
    CHARACTER,
    LOB,
    BOOLEAN,
    DATE,
    JSON
}
