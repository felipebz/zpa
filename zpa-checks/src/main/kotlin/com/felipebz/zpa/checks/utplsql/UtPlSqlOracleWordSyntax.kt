//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

/** The deterministic Java deterministic Java approximation of Oracle's [[:alnum:]_] word class. */
internal object UtPlSqlOracleWordSyntax {

    fun isWordOrDollarOrHash(value: String): Boolean =
        value.isNotEmpty() && value.codePoints().allMatch { codePoint ->
            Character.isLetterOrDigit(codePoint) ||
                codePoint == '_'.code ||
                codePoint == '$'.code ||
                codePoint == '#'.code
        }

    fun isQualifiedWordOrDollarOrHash(value: String): Boolean =
        value.split('.').all(::isWordOrDollarOrHash)
}
