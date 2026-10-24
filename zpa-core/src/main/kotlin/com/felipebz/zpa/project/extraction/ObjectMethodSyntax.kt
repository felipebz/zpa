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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.project.*

/** One method element of an object type specification's element list, from its modifiers to its last token. */
internal class ObjectMethodSyntax(
    private val stream: TokenStream,
    private val typeReferences: TypeReferences,
    private val memberParser: PackageMemberParser
) {
    /** The method in `[start, last]`, or null when the element does not parse. */
    fun parse(start: Int, last: Int): ProjectTypeMethod? {
        var cursor = start
        var overriding = false
        var final = false
        var instantiable = true
        while (cursor <= last) {
            val negated = stream.isKeyword(cursor, "NOT")
            val modifier = if (negated) cursor + 1 else cursor
            when (stream.valueAt(modifier)) {
                "OVERRIDING" -> overriding = !negated
                "FINAL" -> final = !negated
                "INSTANTIABLE" -> instantiable = !negated
                else -> break
            }
            cursor = modifier + 1
        }
        val kind = when (stream.valueAt(cursor)) {
            "MEMBER" -> ProjectMethodKind.MEMBER
            "STATIC" -> ProjectMethodKind.STATIC
            "CONSTRUCTOR" -> ProjectMethodKind.CONSTRUCTOR
            "MAP" -> ProjectMethodKind.MAP.takeIf { stream.isKeyword(cursor + 1, "MEMBER") }
            "ORDER" -> ProjectMethodKind.ORDER.takeIf { stream.isKeyword(cursor + 1, "MEMBER") }
            else -> null
        } ?: return null
        cursor += if (kind == ProjectMethodKind.MAP || kind == ProjectMethodKind.ORDER) 2 else 1
        val function = when (stream.valueAt(cursor)) {
            "FUNCTION" -> true
            "PROCEDURE" -> false
            else -> return null
        }
        if (kind == ProjectMethodKind.CONSTRUCTOR && !function) return null
        val name = stream.identifierAt(cursor + 1) ?: return null
        cursor = name.second
        var parameters = emptyList<ProjectParameter>()
        if (cursor <= last && stream.valueAt(cursor) == "(") {
            val close = stream.strictClose(cursor, last + 1) ?: return null
            parameters = memberParser.parseParameters(cursor + 1, close) ?: return null
            cursor = close + 1
        }
        var returnType: TypeRef? = null
        var returnsSelf = false
        if (function) {
            if (!stream.isKeyword(cursor, "RETURN")) return null
            if (stream.isKeyword(cursor + 1, "SELF") && stream.isKeyword(cursor + 2, "AS") && stream.isKeyword(cursor + 3, "RESULT")) {
                if (kind != ProjectMethodKind.CONSTRUCTOR) return null
                returnsSelf = true
                cursor += 4
            } else {
                if (kind == ProjectMethodKind.CONSTRUCTOR) return null
                val end = stream.firstKeywordAtTopLevel(cursor + 1, PROPERTIES, last + 1) ?: (last + 1)
                returnType = typeReferences.declaredType(cursor + 1, end) ?: return null
                cursor = end
            }
        }
        if (cursor <= last && stream.valueAt(cursor) !in PROPERTIES) return null
        val self = parameters.firstOrNull { it.name == SELF }
        val explicit = parameters.filter { it.name != SELF }.mapIndexed { index, parameter ->
            ProjectParameter(parameter.name, index + 1, parameter.mode, parameter.nocopy, parameter.typeRef, parameter.defaultPresent, parameter.sourceRange)
        }
        return ProjectTypeMethod(
            name.first, kind, overriding, final, instantiable, explicit, self, returnType, returnsSelf, stream.range(start, last)
        )
    }

    private companion object {
        val SELF = OracleIdentifier.fromSource("SELF")

        /** What may follow a method heading in a type specification. */
        val PROPERTIES = setOf("DETERMINISTIC", "PARALLEL_ENABLE", "PIPELINED", "RESULT_CACHE", "AS", "IS", "EXTERNAL", "AGGREGATE", "SQL_MACRO", "ACCESSIBLE")
    }
}
