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

/** Why the callable members of a package specification cannot be bounded by name. */
enum class CallableCoverageGap {
    /** Conditional-compilation text (`$IF`, `$ELSE`, `$END`, ...) the lexer reported as trivia inside the declaration region. */
    CONDITIONAL_COMPILATION,

    /** A SQL*Plus substitution (`&name`) the lexer discarded inside the declaration region. */
    SUBSTITUTION,

    /** Other source text the lexer discarded inside the declaration region. */
    DISCARDED_SOURCE_TEXT,

    /** A dropped `PROCEDURE`/`FUNCTION` header whose name cannot be identified safely. */
    UNIDENTIFIED_CALLABLE_HEADER,

    /** A dropped declaration led by the non-reserved `FUNCTION` that is not a recognizable callable header. */
    AMBIGUOUS_FUNCTION_DECLARATION,

    /** A callable header may have been absorbed by another declaration. */
    SWALLOWED_CALLABLE_HEADER,

    /** Token positions could not be matched with the source text, so discarded text cannot be ruled out. */
    UNVERIFIED_SOURCE_POSITIONS
}

/**
 * Callable coverage of one retained package specification: Oracle-normalized names of callable headers that produced
 * no fact, and the reasons why the callable set may be incomplete beyond those names. Both sets are sorted,
 * runtime-unmodifiable copies, so the marker is immutable and compares deterministically.
 */
class PackageCallableCoverage(omittedNames: Collection<String>, gaps: Collection<CallableCoverageGap>) {
    val omittedNames: Set<String> = Collections.unmodifiableSet(TreeSet(omittedNames))

    val gaps: Set<CallableCoverageGap> =
        Collections.unmodifiableSet(if (gaps.isEmpty()) EnumSet.noneOf(CallableCoverageGap::class.java) else EnumSet.copyOf(gaps))

    /** True when the callable set may be incomplete in a way no name bounds. */
    val unknown: Boolean get() = gaps.isNotEmpty()

    /** True when every callable header of the declaration region produced a fact. */
    val complete: Boolean get() = omittedNames.isEmpty() && gaps.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is PackageCallableCoverage && omittedNames == other.omittedNames && gaps == other.gaps

    override fun hashCode(): Int = 31 * omittedNames.hashCode() + gaps.hashCode()

    override fun toString(): String = "PackageCallableCoverage(omitted=$omittedNames, gaps=$gaps)"

    companion object {
        @JvmField
        val COMPLETE = PackageCallableCoverage(emptyList(), emptyList())
    }
}
