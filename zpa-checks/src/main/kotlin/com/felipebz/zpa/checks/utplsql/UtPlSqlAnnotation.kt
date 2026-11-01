//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.Token

internal enum class UtPlSqlAnnotationArgumentSyntax {
    NONE,
    PARENTHESIZED,
    MALFORMED
}

internal data class UtPlSqlAnnotation(
    val kind: UtPlSqlAnnotationKind,
    val name: String,
    val argument: String?,
    val argumentSyntax: UtPlSqlAnnotationArgumentSyntax,
    val token: Token
)
