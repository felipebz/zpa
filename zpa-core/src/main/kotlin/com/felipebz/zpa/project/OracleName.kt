//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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

        /**
         * The identifier of a token: [lexerValue] is the token's value, which becomes the lookup name when it is
         * exactly that text (the lexer's string is then shared instead of computing a new one); otherwise the lookup
         * name is computed as in [fromSource].
         */
        fun fromToken(originalSpelling: String, lexerValue: String): OracleIdentifier {
            require(originalSpelling.isNotEmpty()) { "An identifier cannot be empty" }
            val length = originalSpelling.length
            val quoted = length >= 2 && originalSpelling.first() == '"' && originalSpelling.last() == '"'
            val lookup = if (quoted) {
                if (lexerValue.length == length - 2 && originalSpelling.regionMatches(1, lexerValue, 0, length - 2)) lexerValue
                else originalSpelling.substring(1, length - 1)
            } else {
                if (isAsciiUppercaseOf(originalSpelling, lexerValue)) lexerValue else originalSpelling.uppercase(Locale.ROOT)
            }
            return OracleIdentifier(originalSpelling, lookup, quoted)
        }

        /**
         * Whether [value] is what `uppercase(Locale.ROOT)` gives for [spelling], proven without creating a string. The
         * lexer uppercases with the default locale, so only ASCII is compared: there, both uppercase `a`-`z` alone.
         */
        private fun isAsciiUppercaseOf(spelling: String, value: String): Boolean {
            if (spelling.length != value.length) return false
            for (i in spelling.indices) {
                val c = spelling[i]
                if (c.code >= 0x80) return false
                val upper = if (c in 'a'..'z') c - ('a' - 'A') else c
                if (value[i] != upper) return false
            }
            return true
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
