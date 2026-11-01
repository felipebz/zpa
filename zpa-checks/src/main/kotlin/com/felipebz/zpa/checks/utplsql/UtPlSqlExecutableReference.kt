//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.PackageProcedureReference

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
