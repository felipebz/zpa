//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.project.OracleIdentifier

/**
 * An exact Oracle data dictionary name as a [ProjectIdentifier]: the name is kept verbatim (never upper-cased, never
 * split on dots), so `EMPLOYEES` equals the source spellings `employees` and `"EMPLOYEES"`, while `Emp` equals only
 * `"Emp"`. The dictionary cannot contain a double quote in a name, so such a name is rejected.
 */
@OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)
internal fun dictionaryIdentifier(name: String, what: String): ProjectIdentifier {
    require(name.isNotEmpty()) { "The $what name must not be empty" }
    require('"' !in name) { "The $what name must not contain a double quote: $name" }
    return ProjectIdentifier.wrap(OracleIdentifier.fromSource("\"$name\""))
}
