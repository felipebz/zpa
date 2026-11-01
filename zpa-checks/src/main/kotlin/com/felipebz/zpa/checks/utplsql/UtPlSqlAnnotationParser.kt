//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.Token
import java.util.*

internal object UtPlSqlAnnotationParser {

    private val annotationNamePattern = Regex("^ *%([A-Za-z][A-Za-z0-9#_$]*)")
    private val annotationArgumentPattern = Regex("^ *%([A-Za-z][A-Za-z0-9#_$]*)[ \\t]*(?:\\((.*)\\)[ \\t]*)$")

    fun parse(token: Token, content: String): UtPlSqlAnnotation? {
        if (!token.originalValue.startsWith("--")) return null

        val nameMatch = annotationNamePattern.find(content) ?: return null
        val name = nameMatch.groupValues[1].lowercase(Locale.ROOT)
        val argumentMatch = annotationArgumentPattern.matchEntire(content)
        val argument = argumentMatch
            ?.groups
            ?.get(2)
            ?.value
            ?.trim()
            ?.takeUnless { it.isEmpty() }
        val trailingText = content.substring(nameMatch.range.last + 1)
        val argumentSyntax = when {
            argumentMatch != null -> UtPlSqlAnnotationArgumentSyntax.PARENTHESIZED
            trailingText.all { it == ' ' || it == '\t' } -> UtPlSqlAnnotationArgumentSyntax.NONE
            else -> UtPlSqlAnnotationArgumentSyntax.MALFORMED
        }

        return UtPlSqlAnnotation(
            kind = UtPlSqlAnnotationKind.from(name),
            name = name,
            argument = argument,
            argumentSyntax = argumentSyntax,
            token = token
        )
    }
}
