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
package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.Token
import java.util.Locale

internal object UtPlSqlAnnotationParser {

    private val annotationPattern = Regex(
        "^ *%([A-Za-z][A-Za-z0-9#_$]*)[ \\t]*(?:\\((.*)\\)[ \\t]*)?$",
        RegexOption.IGNORE_CASE
    )

    fun parse(token: Token, content: String): UtPlSqlAnnotation? {
        if (!token.originalValue.startsWith("--")) return null

        val match = annotationPattern.matchEntire(content) ?: return null
        val name = match.groupValues[1].lowercase(Locale.ROOT)
        val argument = match.groups[2]?.value?.trim()

        return UtPlSqlAnnotation(
            kind = UtPlSqlAnnotationKind.from(name),
            name = name,
            argument = argument,
            token = token
        )
    }
}

