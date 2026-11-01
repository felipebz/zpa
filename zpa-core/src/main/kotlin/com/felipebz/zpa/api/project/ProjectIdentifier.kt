//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.OracleIdentifier

/**
 * One identifier segment. Equality follows Oracle's identifier identity: an unquoted identifier is normalized to its
 * spelling uppercased with `Locale.ROOT`, a quoted identifier keeps its contents (without the surrounding quotes)
 * exactly, and two identifiers are equal iff their normalized names are equal. `run`, `RUN` and `"RUN"` are equal;
 * `"run"` and `"Run"` are different. [originalSpelling] and [isQuoted] describe the source spelling only and do not
 * take part in equality or [hashCode]. Non-ASCII case conversion follows `Locale.ROOT`, not the database character
 * set, and this is not Oracle-complete validation. The numeric hash algorithm is not a public contract.
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
