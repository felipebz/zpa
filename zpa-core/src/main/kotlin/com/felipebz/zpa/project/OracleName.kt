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
package com.felipebz.zpa.project

import java.util.*

/** An Oracle identifier with source spelling and lookup semantics kept separately. */
class OracleIdentifier private constructor(
    val originalSpelling: String,
    val lookupName: String,
    val quoted: Boolean
) {
    companion object {
        fun fromSource(spelling: String): OracleIdentifier {
            require(spelling.isNotEmpty()) { "An identifier cannot be empty" }
            return if (spelling.length >= 2 && spelling.first() == '"' && spelling.last() == '"') {
                OracleIdentifier(spelling, spelling.substring(1, spelling.length - 1), true)
            } else {
                OracleIdentifier(spelling, spelling.uppercase(Locale.ROOT), false)
            }
        }

        fun fromToken(originalSpelling: String, lexerValue: String): OracleIdentifier {
            require(originalSpelling.isNotEmpty()) { "An identifier cannot be empty" }
            val quoted = originalSpelling.length >= 2 && originalSpelling.first() == '"' && originalSpelling.last() == '"'
            val lookup = if (quoted) originalSpelling.substring(1, originalSpelling.length - 1) else originalSpelling.uppercase(Locale.ROOT)
            return OracleIdentifier(originalSpelling, lookup, quoted)
        }
    }

    /**
     * Semantic identity is the normalized [lookupName]: an unquoted identifier is upper-cased and a quoted one keeps
     * its contents, so `run`, `RUN` and `"RUN"` are equal while `"run"` and `"Run"` differ. Source spelling and the
     * [quoted] flag are presentation and are not part of equality; syntactic decisions that depend on quoting (for
     * example built-in datatype recognition) must read [quoted] explicitly.
     */
    override fun equals(other: Any?): Boolean = other is OracleIdentifier && lookupName == other.lookupName

    override fun hashCode(): Int = lookupName.hashCode()

    override fun toString() = originalSpelling
}

class QualifiedName(segments: List<OracleIdentifier>) {
    val segments: List<OracleIdentifier> = immutableList(segments)

    init {
        require(segments.isNotEmpty()) { "A qualified name needs at least one segment" }
    }

    constructor(segment: OracleIdentifier) : this(listOf(segment))

    val last: OracleIdentifier
        get() = segments.last()

    fun append(segment: OracleIdentifier) = QualifiedName(segments + segment)

    /**
     * Semantic key consistent with [equals]: each segment is its length-delimited normalized name, so the encoding is
     * injective even when a quoted segment contains dots or digits, and quoting style does not matter.
     */
    fun lookupKey() = segments.joinToString(".") { "${it.lookupName.length}:${it.lookupName}" }

    override fun toString() = segments.joinToString(".") { it.originalSpelling }

    override fun equals(other: Any?): Boolean = other is QualifiedName && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()
}
