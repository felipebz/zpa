//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Datatype families whose members behave identically when passed to PL/SQL parameters (verified on Oracle 26ai by
 * compiling every member pair against a single procedure). Finer than [ProjectBuiltInTypeCategory], which mixes CLOB
 * with BLOB, DATE with INTERVAL and VARCHAR2 with RAW, ROWID and LONG. A family is evidence for eliminating
 * impossible overloads only; it is not a complete datatype and implies no overload preference.
 */
@ZpaExperimentalApi
enum class ProjectDatatypeFamily {
    /** NUMBER, PLS_INTEGER, BINARY_FLOAT, BINARY_DOUBLE and their predefined subtypes. */
    NUMERIC,

    /** VARCHAR2, CHAR, NVARCHAR2, NCHAR and their predefined subtypes; character literals, including `''`. */
    CHARACTER,

    /** CLOB and NCLOB. */
    CLOB,

    /** BLOB. */
    BLOB,

    /** RAW. */
    RAW,

    /** DATE and every TIMESTAMP variant. */
    DATETIME,

    /** INTERVAL DAY TO SECOND and INTERVAL YEAR TO MONTH, which are not told apart. */
    INTERVAL,

    /** BOOLEAN. */
    BOOLEAN,

    /** JSON. */
    JSON,

    /** ROWID and UROWID. */
    ROWID,

    /** A PL/SQL record or collection, a SQL object or collection, or a `%ROWTYPE` row. */
    COMPOSITE
}
