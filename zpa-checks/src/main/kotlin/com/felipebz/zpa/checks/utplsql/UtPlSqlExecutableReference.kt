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

import com.felipebz.zpa.api.project.PackageProcedureReference
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A single executable reference as written in a utPLSQL annotation argument. */
@OptIn(ZpaExperimentalApi::class)
internal class UtPlSqlExecutableReference(
    val sourceText: String,
    val components: List<String>
) {
    val hasSupportedComponentShape: Boolean
        get() = components.size in 1..3 && components.none(String::isEmpty)

    fun toProjectReference(): PackageProcedureReference? {
        if (!hasSupportedComponentShape) return null
        return when (components.size) {
            1 -> PackageProcedureReference.currentPackage(components[0])
            2 -> PackageProcedureReference.inPackage(components[0], components[1])
            3 -> PackageProcedureReference.inOwnerPackage(components[0], components[1], components[2])
            else -> null
        }
    }
}

/** Parses the executable-list syntax used by utPLSQL without assigning semantic validity. */
internal object UtPlSqlExecutableReferenceParser {

    fun parse(argument: String): List<UtPlSqlExecutableReference> =
        argument.split(',')
            .map(String::trim)
            .filter { reference -> reference.isNotEmpty() && reference.any(Char::isLetter) }
            .map { reference ->
                UtPlSqlExecutableReference(
                    reference,
                    reference.split('.').map(String::trim)
                )
            }
}
