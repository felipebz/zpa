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
package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Whether a declaration is used, as the local name resolution of its file decides it.
 *
 * Only [Unused] is a proven absence of uses. [Unknown] is never one: the analysis could not rule out a use, and a
 * check must not report it as unused.
 */
@ZpaExperimentalApi
sealed interface UsageEvidence {
    /** A reference of the file certainly names the declaration. */
    object Used : UsageEvidence {
        override fun toString() = "Used"
    }

    /**
     * Proven: no reference names the declaration, and every region the name is visible in is complete (nothing the
     * analysis cannot see may hold a use).
     */
    object Unused : UsageEvidence {
        override fun toString() = "Unused"
    }

    /** Neither a certain use nor a proven absence; [reasons] says what could hold a use. Never a proven absence. */
    class Unknown(reasons: Set<UsageUncertainty>) : UsageEvidence {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence at all: see [reason]. A check reports nothing for such a declaration. */
    class Unavailable(val reason: UsageUnavailability) : UsageEvidence {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/** What may hold a use the analysis could not decide. */
@ZpaExperimentalApi
enum class UsageUncertainty {
    /** A name in SQL that names the declaration unless the statement's relations have a column of that name. */
    SQL_NAME_MAY_BE_COLUMN,

    /**
     * A reference names the declaration, but its lookup relied on a declaration outside the file (a package
     * specification or a supertype) that may declare the same name.
     */
    DECLARATION_OUTSIDE_FILE,

    /** Conditional-compilation text touches a region the name is visible in. */
    CONDITIONAL_COMPILATION,

    /** A SQL*Plus substitution (`&name`) touches a region the name is visible in. */
    SUBSTITUTION,

    /** The parser recovered from a syntax error in a region the name is visible in. */
    PARSE_RECOVERY,

    /** Other source text the analysis discarded, or a declaration it could not extract, in a region the name is visible in. */
    INCOMPLETE_SOURCE,

    /** A reference of the same name that the analysis does not model is written where the declaration is visible. */
    UNSUPPORTED_REFERENCE,

    /** The declaration is visible outside the file (a package specification member): other units may use it. */
    VISIBLE_OUTSIDE_FILE
}

/** Why there is no usage evidence. */
@ZpaExperimentalApi
enum class UsageUnavailability {
    /** The local analysis of the file did not complete (a failure was logged): nothing is known about it. */
    FILE_NOT_ANALYZED,

    /**
     * The node is not a declaration this evidence covers: not a variable, exception, cursor, parameter or cursor
     * parameter declaration, or one the local analysis holds no declaration for (a call specification's parameters).
     */
    NOT_A_COVERED_DECLARATION
}
