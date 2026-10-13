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

/** Kind of a package specification member whose name was identified without producing a declaration fact. */
enum class PackageMemberKind {
    CURSOR,
    VALUE,
    EXCEPTION,
    TYPE,
    SUBTYPE,
    PROCEDURE,
    FUNCTION
}

/** Why the member names of a package specification cannot be bounded: a missing name is then not proof of absence. */
enum class MemberNameGap {
    /** Conditional-compilation text the lexer discarded inside the declaration region. */
    CONDITIONAL_COMPILATION,

    /** A SQL*Plus substitution the lexer discarded inside the declaration region. */
    SUBSTITUTION,

    /** Other source text the lexer discarded inside the declaration region. */
    DISCARDED_SOURCE_TEXT,

    /** Token positions could not be matched with the source text. */
    UNVERIFIED_SOURCE_POSITIONS,

    /** A declaration produced no fact and its leading name could not be identified. */
    UNIDENTIFIED_DECLARATION,

    /** A declaration had no `;` before the next subprogram: text between them may hide names. */
    UNTERMINATED_DECLARATION,

    /** A declaration may have absorbed the heading of another one (for example after a missing `;`). */
    SWALLOWED_DECLARATION
}

/** One member name of a specification that produced no fact, with the range of its whole declaration. */
class PackageMemberName(val name: OracleIdentifier, val kind: PackageMemberKind, val sourceRange: SourceRange) {
    override fun equals(other: Any?): Boolean =
        other is PackageMemberName && name == other.name && kind == other.kind && sourceRange == other.sourceRange

    override fun hashCode(): Int = Objects.hash(name, kind, sourceRange)

    override fun toString(): String = "$kind $name @ $sourceRange"
}

/**
 * Name coverage of one retained package specification. Members that produced facts are not repeated here: they are the
 * indexed occurrences inside the specification. [unretained] holds the identified names of every other declaration
 * (cursors, exceptions, dropped values, types or callables), in source order; [gaps] says why other names may exist.
 */
class PackageMemberNameCoverage(
    unretained: List<PackageMemberName>,
    gaps: Collection<MemberNameGap>,
    /**
     * Range of the tokens between the first and the last conditional-compilation directive the lexer discarded. The
     * lexer keeps the tokens of `$THEN` branches, so a member overlapping this range may be absent for Oracle.
     */
    val conditionalRange: SourceRange? = null
) {
    val unretained: List<PackageMemberName> = Collections.unmodifiableList(ArrayList(unretained))

    val gaps: Set<MemberNameGap> =
        Collections.unmodifiableSet(if (gaps.isEmpty()) EnumSet.noneOf(MemberNameGap::class.java) else EnumSet.copyOf(gaps))

    /** True when every declaration of the region has a fact or an identified name. */
    val complete: Boolean get() = gaps.isEmpty()

    /** True when the declaration [range] is present in every compilation of the specification. */
    fun isUnconditional(range: SourceRange): Boolean {
        if (MemberNameGap.UNVERIFIED_SOURCE_POSITIONS in gaps) return false
        val conditional = conditionalRange ?: return true
        return !after(range.endLine, range.endColumn, conditional.startLine, conditional.startColumn) ||
            !after(conditional.endLine, conditional.endColumn, range.startLine, range.startColumn)
    }

    /** (line, column) a is strictly after b. */
    private fun after(aLine: Int, aColumn: Int, bLine: Int, bColumn: Int) = aLine > bLine || (aLine == bLine && aColumn > bColumn)

    override fun equals(other: Any?): Boolean =
        other is PackageMemberNameCoverage && unretained == other.unretained && gaps == other.gaps &&
            conditionalRange == other.conditionalRange

    override fun hashCode(): Int = Objects.hash(unretained, gaps, conditionalRange)

    override fun toString(): String = "PackageMemberNameCoverage(unretained=$unretained, gaps=$gaps, conditionalRange=$conditionalRange)"
}

/**
 * What a package specification says about a member name at a reference. Oracle 26ai rejects every reference whose
 * first segment matches a member declared later in the same specification, or whose own declaration contains it, so
 * only [DECLARED_BEFORE] is a capture; both exclude a schema-level reading of the name. Members inside a
 * conditional-compilation span count only when the outcome is the same with or without them.
 */
enum class MemberNameStatus {
    /** Some unconditional member, and every member with the name, is completely declared before the reference. */
    DECLARED_BEFORE,

    /** Some unconditional member with the name is declared after the reference or contains it: Oracle rejects it. */
    REJECTED,

    /** No member has the name, and the specification's name coverage is complete. */
    NOT_DECLARED,

    /** Presence or order of the name is not proven: gaps, conditional members, or coverage not recorded. */
    UNKNOWN
}

/** The members matched for a name, and the resulting status. */
class MemberNameEvidence(val status: MemberNameStatus, members: List<SourceRange>) {
    /** Declaration ranges of the matching members (facts and unretained names), in source order. */
    val members: List<SourceRange> = Collections.unmodifiableList(ArrayList(members))
}

/** Member-name evidence of one package specification occurrence; positions are compared only within that occurrence. */
object PackageMemberNames {
    /**
     * Evidence for [name] in the specification [unit] (an indexed package specification occurrence). [reference] is
     * the range of a reference written inside that same specification, or null for a reference outside it (a package
     * body or a caller), where every specification member precedes the reference.
     */
    fun at(index: ProjectSymbolIndex, unit: IndexedDeclaration, name: OracleIdentifier, reference: SourceRange?): MemberNameEvidence {
        val spec = unit.declaration as PackageDeclaration
        require(spec.role == DeclarationRole.SPECIFICATION) { "A package specification occurrence is required" }
        require(reference == null || (reference.fileId == spec.fileId && contains(spec.sourceRange, reference))) {
            "The reference must lie inside the specification"
        }
        val retained = index.findOccurrences(spec.name.append(name))
            .map { it.declaration }
            .filter { it !is PackageDeclaration && it.fileId == spec.fileId && contains(spec.sourceRange, it.sourceRange) }
            .map { it.sourceRange }
        val unretained = spec.memberNames?.unretained.orEmpty().filter { it.name == name }.map { it.sourceRange }
        val members = (retained + unretained).sortedWith(compareBy({ it.startLine }, { it.startColumn }))
        val coverage = spec.memberNames
        val (certain, conditional) = members.partition { coverage?.isUnconditional(it) ?: false }
        val status = when {
            reference != null && certain.any { !completedBefore(it, reference) } -> MemberNameStatus.REJECTED
            certain.isNotEmpty() && (reference == null || conditional.all { completedBefore(it, reference) }) ->
                MemberNameStatus.DECLARED_BEFORE
            members.isEmpty() && coverage?.complete == true -> MemberNameStatus.NOT_DECLARED
            else -> MemberNameStatus.UNKNOWN
        }
        return MemberNameEvidence(status, members)
    }

    private fun completedBefore(member: SourceRange, reference: SourceRange): Boolean =
        member.endLine < reference.startLine || (member.endLine == reference.startLine && member.endColumn <= reference.startColumn)

    private fun contains(outer: SourceRange, inner: SourceRange): Boolean =
        (outer.startLine < inner.startLine || (outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn)) &&
            (inner.endLine < outer.endLine || (inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn))
}
