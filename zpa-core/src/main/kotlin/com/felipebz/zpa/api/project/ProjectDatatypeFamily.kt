/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
