//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols.datatype

import com.felipebz.zpa.api.symbols.PlSqlType

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. For a name use
 * PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node. Length,
 * precision and scale are read from the DATATYPE node of the declaration.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. For a name use " +
    "PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node. " +
    "Length, precision and scale are read from the DATATYPE node of the declaration."
)
object UnknownDatatype : PlSqlDatatype {
    override val type = PlSqlType.UNKNOWN

    override val name: String? = null

    override fun toString(): String {
        return "Unknown"
    }
}
