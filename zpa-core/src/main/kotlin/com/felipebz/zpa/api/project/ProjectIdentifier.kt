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
import com.felipebz.zpa.project.OracleIdentifier

/**
 * One identifier segment. Unquoted identifiers are equal iff their spellings uppercased with
 * `Locale.ROOT` are equal; quoted identifiers are equal iff their contents (without the
 * surrounding quotes) are exactly equal; quoted and unquoted identifiers are never equal.
 * This preserves current Project System name equivalence and is not Oracle-complete validation.
 * The numeric hash algorithm is not a public contract.
 */
@ZpaExperimentalApi
class ProjectIdentifier private constructor(@get:JvmSynthetic internal val identifier: OracleIdentifier) {
    /** Source spelling, including quotes and original case. */
    val originalSpelling: String get() = identifier.originalSpelling

    val isQuoted: Boolean get() = identifier.quoted

    override fun equals(other: Any?): Boolean = other is ProjectIdentifier && identifier == other.identifier

    override fun hashCode(): Int = identifier.hashCode()

    override fun toString(): String = identifier.originalSpelling

    companion object {
        /**
         * @param spelling one identifier segment as written in source; never split on dots
         * @throws IllegalArgumentException if empty
         */
        @JvmStatic
        fun fromSource(spelling: String): ProjectIdentifier = ProjectIdentifier(OracleIdentifier.fromSource(spelling))

        @JvmSynthetic
        internal fun wrap(identifier: OracleIdentifier): ProjectIdentifier = ProjectIdentifier(identifier)
    }
}
