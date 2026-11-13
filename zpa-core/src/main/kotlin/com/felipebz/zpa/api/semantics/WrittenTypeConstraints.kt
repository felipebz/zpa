//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * The constraints written after a predefined type's name, placed as the data dictionary reports a column of that type:
 * - [length] and [lengthSemantics]: `VARCHAR2(30 CHAR)`, `RAW(16)`, `NCHAR(5)`;
 * - [precision] and [scale]: `NUMBER(15, 3)`, `FLOAT(63)`;
 * - [scale]: the fractional seconds of `TIMESTAMP(3)`, with or without a time zone;
 * - [precision] and [scale]: the leading field and the fractional seconds of `INTERVAL DAY(2) TO SECOND(6)`, and
 *   [precision] alone for `INTERVAL YEAR(4) TO MONTH`.
 *
 * Each is null when it is not written: Oracle then applies the type's default (`NUMBER` has no precision limit,
 * `TIMESTAMP` six fractional digits, `VARCHAR2` in PL/SQL needs a length). [lengthSemantics] is null when neither
 * `BYTE` nor `CHAR` is written: the session's `NLS_LENGTH_SEMANTICS` decides it.
 */
@ZpaExperimentalApi
class WrittenTypeConstraints internal constructor(
    val length: WrittenTypeConstraint?,
    val lengthSemantics: LengthSemantics?,
    val precision: WrittenTypeConstraint?,
    val scale: WrittenTypeConstraint?
) {
    override fun equals(other: Any?) = other is WrittenTypeConstraints && length == other.length &&
        lengthSemantics == other.lengthSemantics && precision == other.precision && scale == other.scale

    override fun hashCode() = listOf(length, lengthSemantics, precision, scale).hashCode()

    override fun toString() = listOfNotNull(
        length?.let { "length=$it" + (lengthSemantics?.let { semantics -> " $semantics" } ?: "") },
        precision?.let { "precision=$it" },
        scale?.let { "scale=$it" }
    ).joinToString(", ", "(", ")")

    internal companion object {
        val NONE = WrittenTypeConstraints(null, null, null, null)
    }
}

/** One written constraint. */
@ZpaExperimentalApi
sealed interface WrittenTypeConstraint {
    /** An integer, written as a literal or as a constant expression the analysis evaluated. */
    class Value(val value: Int) : WrittenTypeConstraint {
        override fun equals(other: Any?) = other is Value && value == other.value

        override fun hashCode() = value

        override fun toString() = value.toString()
    }

    /** The `*` of `NUMBER(*, 2)`: the maximum precision. */
    data object Maximum : WrittenTypeConstraint

    /** Written as an expression whose value the analysis does not decide. Present, never guessed. */
    data object Unknown : WrittenTypeConstraint
}

/** Whether a length counts bytes or characters. */
@ZpaExperimentalApi
enum class LengthSemantics { BYTE, CHAR }
