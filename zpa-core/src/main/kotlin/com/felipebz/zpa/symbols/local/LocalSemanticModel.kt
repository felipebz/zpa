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
import java.util.*

/*
 * Per-file local semantic model: the lexical regions of one parsed file and the declarations they own, as immutable,
 * AST-detached facts. Internal and not consumed yet. It records what was written; it does not resolve names. Whether
 * the absence of a declaration from a region can be trusted is [LocalRegion.completeness].
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

/** How much of a region's own declarations the builder extracted. Never a statement that the list is complete. */
internal enum class LocalExtraction {
    /** Every declaration item the AST shows for the region was extracted (the AST itself may be incomplete). */
    ALL_ITEMS_EXTRACTED,

    /** Some declaration items, recorded as [LocalCoverageGap]s, were not extracted. */
    PARTIAL
}

internal enum class LocalGapKind {
    /** A declaration item of a form the model does not extract. */
    UNSUPPORTED_DECLARATION,

    /** Source the parser skipped while recovering from a syntax error; it may have held declarations. */
    PARSE_RECOVERY
}

/** Evidence that something written in a region was not extracted. [syntax] names the grammar rule. */
internal class LocalCoverageGap(val kind: LocalGapKind, val syntax: String, val range: SourceRange) {
    override fun toString() = "$kind $syntax@${range.startLine}:${range.startColumn}"
}

/** Why a region's declarations may be incomplete. Declared in the order reasons are reported. */
internal enum class LocalIncompleteness {
    /** The region holds a declaration item the builder did not extract ([LocalExtraction.PARTIAL]). */
    PARTIAL_EXTRACTION,

    /**
     * Conditional-compilation text touches the region: a skipped branch may hold a declaration, or a kept branch
     * (which may not exist for Oracle) overlaps it.
     */
    CONDITIONAL_COMPILATION,

    /** A SQL*Plus substitution (`&name`) the lexer discarded touches the region. */
    SUBSTITUTION,

    /** Other source text the lexer discarded touches the region. */
    DISCARDED_SOURCE_TEXT,

    /** A token could not be located in the source text, so discarded text cannot be ruled out anywhere. */
    UNVERIFIED_SOURCE_POSITIONS,

    /** The source text was not supplied, so discarded text cannot be ruled out anywhere. */
    SOURCE_TEXT_UNAVAILABLE,

    /** The parser recovered from a syntax error where the region may lose or gain declarations. */
    PARSE_RECOVERY
}

/**
 * Whether every declaration Oracle could see in a region's own text (its declarations and labels, the declaration
 * items of its declarative part and the parameters of its heading) is in the model: the declaration-coverage,
 * conditional-compilation and recovery conditions of a proven absence hold for this region. It says nothing about the
 * lookup-time conditions (stopping outcomes, qualification, SQL namespace), which a resolver checks.
 *
 * Completeness is relative to the configured conditional-compilation settings, the same ones the AST follows:
 * - without active settings, the lexer keeps the first branch of each selection without evaluating its condition, so
 *   every region conditional compilation touches is incomplete;
 * - with active settings, a selection whose conditions all evaluate `FALSE`/`NULL` keeps nothing and is reported as a
 *   directive, which is not evidence: a region whose skipped branch holds declarations is then complete for those
 *   settings. A branch kept because its condition is proven `TRUE` still makes the region incomplete (conservative:
 *   the lexer does not report how a branch was chosen).
 *
 * A region no conditional-compilation text touches is independent of the settings.
 */
internal class LocalRegionCompleteness private constructor(
    /** Empty when complete; otherwise the reasons, in [LocalIncompleteness] order. */
    val reasons: Set<LocalIncompleteness>
) {
    val isComplete: Boolean get() = reasons.isEmpty()

    override fun toString() = if (isComplete) "COMPLETE" else "INCOMPLETE$reasons"

    companion object {
        val COMPLETE = LocalRegionCompleteness(emptySet())

        fun of(reasons: Set<LocalIncompleteness>): LocalRegionCompleteness =
            if (reasons.isEmpty()) COMPLETE else LocalRegionCompleteness(Collections.unmodifiableSet(EnumSet.copyOf(reasons)))
    }
}

internal class LocalRegion(
    val id: LocalRegionId,
    /** Null only for the [LocalRegionKind.FILE] root. */
    val parent: LocalRegionId?,
    val kind: LocalRegionKind,
    /** The grammar rule that forms the region (`BLOCK_STATEMENT`, `FOR_STATEMENT`…; the root's is `FILE`). */
    val syntax: String,
    /** From the region's first token (after any label, which belongs to the enclosing region) to its last. */
    val range: SourceRange,
    /** The region's own declarations, in source order. */
    val declarations: List<LocalDeclarationId>,
    /** Directly nested regions, in source order. */
    val children: List<LocalRegionId>,
    val extraction: LocalExtraction,
    val gaps: List<LocalCoverageGap>,
    val completeness: LocalRegionCompleteness
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
    /** The declaration item, from its first token to its last. */
    val range: SourceRange,
    val nameRange: SourceRange,
    /**
     * The declared datatype of a variable, subtype, parameter, cursor parameter or loop index, or the element type
     * of a collection type. A name such as `NUMBER` is only a written name here: a local declaration may shadow it.
     * Kind-specific types (RETURN, INDEX BY, fields) are in [details].
     */
    val type: LocalTypeUse?,
    /** `CONSTANT` (variables only). */
    val isConstant: Boolean,
    /** `NOT NULL` on the declared type (or on the element type of a collection). */
    val isNotNull: Boolean,
    /** A default value (`:=` or `DEFAULT`). */
    val hasDefault: Boolean,
    val details: LocalDeclarationDetails?
) {
    val typeRef: TypeRef? get() = type?.typeRef

    override fun toString() = "$kind $name $id in $region"
}

internal class LocalSemanticModel(
    val fileId: FileId,
    /** Indexed by [LocalRegionId.ordinal]; the root is first. */
    val regions: List<LocalRegion>,
    /** Indexed by [LocalDeclarationId.ordinal]. */
    val declarations: List<LocalDeclaration>
) {
    val root: LocalRegion get() = regions[0]

    fun region(id: LocalRegionId): LocalRegion = regions[id.ordinal]

    fun declaration(id: LocalDeclarationId): LocalDeclaration = declarations[id.ordinal]

    fun children(region: LocalRegionId): List<LocalRegion> = region(region).children.map(::region)

    fun declarationsOf(region: LocalRegionId): List<LocalDeclaration> = region(region).declarations.map(::declaration)

    fun parent(region: LocalRegionId): LocalRegion? = region(region).parent?.let(::region)

    /** Lookup tables for the resolver, derived from the regions and declarations on first use. */
    val index: LocalLookupIndex by lazy { LocalLookupIndex(this) }

    override fun toString() = "LocalSemanticModel($fileId, ${regions.size} regions, ${declarations.size} declarations)"
}
