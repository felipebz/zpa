package com.felipebz.zpa.project

import java.util.Locale

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

    /** Oracle identity ignores source spelling for unquoted names but not for quoted names. */
    override fun equals(other: Any?): Boolean =
        other is OracleIdentifier && quoted == other.quoted && lookupName == other.lookupName

    override fun hashCode(): Int = 31 * lookupName.hashCode() + quoted.hashCode()

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

    fun lookupKey() = segments.joinToString(".") { "${it.quoted}:\u0000${it.lookupName}" }

    override fun toString() = segments.joinToString(".") { it.originalSpelling }

    override fun equals(other: Any?): Boolean = other is QualifiedName && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()
}
