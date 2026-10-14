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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.TypeRef

/*
 * Per-file local semantic model: the lexical regions of one parsed file and the declarations they own, as immutable,
 * AST-detached facts. Internal and not consumed yet. It records what was written; it does not resolve names, and
 * nothing in it proves that a name is absent from a region (see LocalCompleteness).
 */

/** A region's position in [LocalSemanticModel.regions]: document order of the region's start, the file region first. */
@JvmInline
internal value class LocalRegionId(val ordinal: Int) {
    override fun toString() = "R$ordinal"
}

/** A declaration's position in [LocalSemanticModel.declarations]: document order over the whole file. */
@JvmInline
internal value class LocalDeclarationId(val ordinal: Int) {
    override fun toString() = "D$ordinal"
}

internal enum class LocalRegionKind {
    /** The whole file: the root, holding the top-level units. It declares nothing itself. */
    FILE,

    /** A `[DECLARE \u2026] BEGIN \u2026 END` block, top-level or nested in a statement list. */
    ANONYMOUS_BLOCK,

    /**
     * A construct that forms a lexical region the model does not extract yet (subprograms, FOR loops, packages,
     * triggers, type bodies\u2026). Kept so that regions nested in it have their true parent; its own declarations are
     * not extracted.
     */
    UNMODELED
}

internal enum class LocalDeclarationKind { VARIABLE, SUBTYPE }

/** How much of a region's own declarations the builder extracted. Never a statement that the list is complete. */
internal enum class LocalExtraction {
    /** Every declaration item the AST shows for the region was extracted (the AST itself may be incomplete). */
    ALL_ITEMS_EXTRACTED,

    /** Some declaration items, recorded as [LocalCoverageGap]s, were not extracted. */
    PARTIAL,

    /** The region's declarations are not extracted at all ([LocalRegionKind.UNMODELED]). */
    NOT_EXTRACTED
}

internal enum class LocalGapKind {
    /** A declaration item of a form the model does not extract (exception, cursor, subprogram, record type\u2026). */
    UNSUPPORTED_DECLARATION,

    /** A `<<label>>`: a declaration of the region holding the labeled statement or block, not extracted yet. */
    LABEL,

    /** Source the parser skipped while recovering from a syntax error; it may have held declarations. */
    PARSE_RECOVERY
}

/** Evidence that something written in a region was not extracted. [syntax] names the grammar rule. */
internal class LocalCoverageGap(val kind: LocalGapKind, val syntax: String, val range: SourceRange) {
    override fun toString() = "$kind $syntax@${range.startLine}:${range.startColumn}"
}

/**
 * Whether the absence of a declaration from the model could ever be trusted. In this version it never can: the
 * evidence needed (conditional compilation, recovery attribution) is not established yet.
 */
internal enum class LocalCompleteness {
    /** No completeness evidence exists for the file; nothing observed contradicts the extracted facts either. */
    NOT_ESTABLISHED,

    /** The parser recovered from syntax errors: any region may be missing declarations, whatever its AST shows. */
    PARSE_RECOVERY_OBSERVED
}

internal class LocalRegion(
    val id: LocalRegionId,
    /** Null only for the [LocalRegionKind.FILE] root. */
    val parent: LocalRegionId?,
    val kind: LocalRegionKind,
    /** The grammar rule that forms the region (`BLOCK_STATEMENT`, `FOR_STATEMENT`\u2026; the root's is `FILE`). */
    val syntax: String,
    /** From the region's first token (after any label, which belongs to the enclosing region) to its last. */
    val range: SourceRange,
    /** The region's own declarations, in source order. */
    val declarations: List<LocalDeclarationId>,
    /** Directly nested regions, in source order. */
    val children: List<LocalRegionId>,
    val extraction: LocalExtraction,
    val gaps: List<LocalCoverageGap>
) {
    override fun toString() = "$kind $id${parent?.let { " in $it" } ?: ""} ${declarations.size} declarations, $extraction"
}

internal class LocalDeclaration(
    val id: LocalDeclarationId,
    val region: LocalRegionId,
    /** Index in the owning region's [LocalRegion.declarations]. */
    val position: Int,
    val kind: LocalDeclarationKind,
    val name: OracleIdentifier,
    /** The declaration item, from its first token to its terminating semicolon. */
    val range: SourceRange,
    val nameRange: SourceRange,
    /** Where the declared datatype is written. */
    val typeRange: SourceRange,
    /**
     * The written datatype as unresolved syntax, or null when it cannot be represented as a [TypeRef]. A name such as
     * `NUMBER` is only a written name here: a local declaration may shadow it.
     */
    val typeRef: TypeRef?,
    /** `CONSTANT` (variables only). */
    val isConstant: Boolean,
    /** `NOT NULL`. */
    val isNotNull: Boolean
) {
    override fun toString() = "$kind $name $id in $region"
}

internal class LocalSemanticModel(
    val fileId: FileId,
    /** Indexed by [LocalRegionId.ordinal]; the root is first. */
    val regions: List<LocalRegion>,
    /** Indexed by [LocalDeclarationId.ordinal]. */
    val declarations: List<LocalDeclaration>,
    val completeness: LocalCompleteness
) {
    val root: LocalRegion get() = regions[0]

    fun region(id: LocalRegionId): LocalRegion = regions[id.ordinal]

    fun declaration(id: LocalDeclarationId): LocalDeclaration = declarations[id.ordinal]

    fun children(region: LocalRegionId): List<LocalRegion> = region(region).children.map(::region)

    fun declarationsOf(region: LocalRegionId): List<LocalDeclaration> = region(region).declarations.map(::declaration)

    fun parent(region: LocalRegionId): LocalRegion? = region(region).parent?.let(::region)

    override fun toString() = "LocalSemanticModel($fileId, ${regions.size} regions, ${declarations.size} declarations, $completeness)"
}
