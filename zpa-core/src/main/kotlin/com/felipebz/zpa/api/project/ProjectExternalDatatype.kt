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
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/**
 * Detached datatype of an external (data dictionary) column, with the dictionary values as supplied. An absent value
 * means SQL NULL or "not applicable" and is never replaced by zero. It is not a source type reference and carries no
 * nullability, default, constraint or domain.
 */
@ZpaExperimentalApi
class ProjectExternalDatatype private constructor(
    /** `DATA_TYPE` verbatim, for example `NUMBER`, `TIMESTAMP(3)` or `INTERVAL DAY(2) TO SECOND(6)`. */
    val name: String,
    typeOwner: ProjectIdentifier?,
    length: Int?,
    lengthSemantics: LengthSemantics?,
    precision: Int?,
    scale: Int?
) {
    enum class LengthSemantics { BYTE, CHAR }

    /**
     * `DATA_TYPE_OWNER` as an exact dictionary name, preserved as supplied.
     * For named types, the value may reflect how the type was referenced
     * in the column declaration, including through a synonym.
     */
    val typeOwner: Optional<ProjectIdentifier> = Optional.ofNullable(typeOwner)

    /** Declared length in [lengthSemantics] units (character length for character types, bytes for `RAW`). */
    val length: OptionalInt = length?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    val lengthSemantics: Optional<LengthSemantics> = Optional.ofNullable(lengthSemantics)

    /** `DATA_PRECISION` with dictionary meaning (binary digits for `FLOAT`, leading field precision for `INTERVAL`). */
    val precision: OptionalInt = precision?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    /** `DATA_SCALE` with dictionary meaning (may be negative; fractional seconds precision for datetime types). */
    val scale: OptionalInt = scale?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    /**
     * The existing built-in category recognized from the base name (the text before `(`), only for types without an
     * owner; `TIMESTAMP…` and `INTERVAL…` are [ProjectBuiltInTypeCategory.DATE]. Empty means the type is known but not
     * represented by a category (object types, `XMLTYPE`, `VECTOR`, `LONG RAW`, ...); its name and owner remain reliable.
     */
    val category: Optional<ProjectBuiltInTypeCategory> = Optional.ofNullable(if (typeOwner == null) categoryOf(name) else null)

    override fun equals(other: Any?): Boolean = other is ProjectExternalDatatype && name == other.name &&
        typeOwner == other.typeOwner && length == other.length && lengthSemantics == other.lengthSemantics &&
        precision == other.precision && scale == other.scale

    override fun hashCode(): Int = Objects.hash(name, typeOwner, length, lengthSemantics, precision, scale)

    override fun toString(): String = buildString {
        typeOwner.ifPresent { append(it.originalSpelling).append('.') }
        append(name)
        val args = listOfNotNull(
            this@ProjectExternalDatatype.length.takeIf { it.isPresent }?.let { "${it.asInt}" + lengthSemantics.map { s -> " $s" }.orElse("") },
            precision.takeIf { it.isPresent }?.let { "p=${it.asInt}" },
            scale.takeIf { it.isPresent }?.let { "s=${it.asInt}" }
        )
        if (args.isNotEmpty()) append(args.joinToString(", ", "[", "]"))
    }

    companion object {
        /**
         * @param dataType `DATA_TYPE`, nonblank
         * @param dataTypeOwner `DATA_TYPE_OWNER` as an exact dictionary name, or null
         * @param length declared length (non-negative), or null
         * @param lengthSemantics the unit of [length]; requires a [length]
         * @param precision `DATA_PRECISION` (non-negative), or null for SQL NULL
         * @param scale `DATA_SCALE` (any sign), or null for SQL NULL
         * @throws IllegalArgumentException for a blank name or owner, a negative length or precision, or length
         *   semantics without a length
         */
        @JvmStatic
        fun of(
            dataType: String,
            dataTypeOwner: String?,
            length: Int?,
            lengthSemantics: LengthSemantics?,
            precision: Int?,
            scale: Int?
        ): ProjectExternalDatatype {
            require(dataType.isNotBlank()) { "The datatype name must not be blank" }
            require(length == null || length >= 0) { "The length must not be negative: $length" }
            require(lengthSemantics == null || length != null) { "Length semantics require a length" }
            require(precision == null || precision >= 0) { "The precision must not be negative: $precision" }
            val owner = dataTypeOwner?.let {
                require(it.isNotBlank()) { "The datatype owner must not be blank" }
                dictionaryIdentifier(it, "datatype owner")
            }
            return ProjectExternalDatatype(dataType, owner, length, lengthSemantics, precision, scale)
        }

        private fun categoryOf(dataType: String): ProjectBuiltInTypeCategory? {
            val base = dataType.substringBefore('(').trim()
            if (base == "TIMESTAMP" || base.startsWith("TIMESTAMP ") || base == "INTERVAL" || base.startsWith("INTERVAL ")) {
                return ProjectBuiltInTypeCategory.DATE
            }
            if (base.isEmpty() || base.any { it.isWhitespace() || it == '"' }) return null
            return builtInCategory(QualifiedName(OracleIdentifier.fromSource(base)))
        }
    }
}
