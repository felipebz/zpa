//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.symbols

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. For a built-in type name use
 * ProjectBuiltInTypeCategory; otherwise, for a name PlSqlVisitorContext.valueCategory, for a DATATYPE node
 * typeCategory, and for a literal its grammar node.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. For a built-in type name use " +
    "ProjectBuiltInTypeCategory; otherwise, for a name PlSqlVisitorContext.valueCategory, for a DATATYPE node " +
    "typeCategory, and for a literal its grammar node."
)
enum class PlSqlType {

    UNKNOWN,
    CHARACTER,
    NUMERIC,
    DATE,
    LOB,
    BOOLEAN,
    ROWTYPE,
    ASSOCIATIVE_ARRAY,
    NULL,
    RECORD,
    JSON,
    EXCEPTION;

    val isCharacter: Boolean
        get() = this == CHARACTER

    val isNumeric: Boolean
        get() = this == NUMERIC

    val isUnknown: Boolean
        get() = this == UNKNOWN

}
