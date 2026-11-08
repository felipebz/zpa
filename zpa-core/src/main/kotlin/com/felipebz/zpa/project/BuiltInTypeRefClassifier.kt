//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory

/**
 * Built-in classification that can be applied to the information retained in a NamedTypeRef.
 *
 * The parser grammar remains the authoritative source for the accepted spellings and for
 * datatype constraints. This classifier only provides the broad built-in category that is
 * still recoverable after TypeRef extraction. Qualified and quoted names are deliberately
 * excluded because their meaning is namespace- or identifier-dependent.
 */
internal object BuiltInTypeRefClassifier {
    fun classify(name: QualifiedName): ProjectBuiltInTypeCategory? {
        if (name.segments.size != 1) return null
        val identifier = name.last
        if (identifier.quoted) return null
        return classifyLookupName(identifier.lookupName)
    }

    private fun classifyLookupName(lookupName: String): ProjectBuiltInTypeCategory? {
        return when (lookupName) {
            "BINARY_DOUBLE", "BINARY_FLOAT", "BINARY_INTEGER", "DEC", "DECIMAL",
            "DOUBLE", "FLOAT", "INT", "INTEGER", "NATURAL", "NATURALN", "NUMBER",
            "NUMERIC", "PLS_INTEGER", "POSITIVE", "POSITIVEN", "REAL", "SIGNTYPE",
            "SMALLINT", "SIMPLE_INTEGER", "SIMPLE_FLOAT", "SIMPLE_DOUBLE" -> ProjectBuiltInTypeCategory.NUMERIC

            "CHAR", "CHARACTER", "LONG", "NCHAR", "NVARCHAR2", "RAW", "ROWID",
            "STRING", "UROWID", "VARCHAR", "VARCHAR2",
            "DBMS_ID", "DBMS_QUOTED_ID", "DBMS_ID_30", "DBMS_QUOTED_ID_30", "DBMS_ID_128", "DBMS_QUOTED_ID_128" -> ProjectBuiltInTypeCategory.CHARACTER

            "BFILE", "BLOB", "CLOB", "NCLOB" -> ProjectBuiltInTypeCategory.LOB
            "BOOLEAN", "BOOL" -> ProjectBuiltInTypeCategory.BOOLEAN
            "DATE", "INTERVAL", "TIMESTAMP", "TIMESTAMP_UNCONSTRAINED", "TIMESTAMP_TZ_UNCONSTRAINED", "TIMESTAMP_LTZ_UNCONSTRAINED",
            "DSINTERVAL_UNCONSTRAINED", "YMINTERVAL_UNCONSTRAINED" -> ProjectBuiltInTypeCategory.DATE
            "JSON" -> ProjectBuiltInTypeCategory.JSON
            else -> null
        }
    }
}
