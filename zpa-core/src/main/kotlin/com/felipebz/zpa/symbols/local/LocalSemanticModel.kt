//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.UsageUncertainty
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

/** The usage uncertainty an incomplete region gives the answers that rely on it. */
@OptIn(ZpaExperimentalApi::class)
internal fun LocalIncompleteness.usageUncertainty(): UsageUncertainty = when (this) {
    LocalIncompleteness.CONDITIONAL_COMPILATION -> UsageUncertainty.CONDITIONAL_COMPILATION
    LocalIncompleteness.SUBSTITUTION -> UsageUncertainty.SUBSTITUTION
    LocalIncompleteness.PARSE_RECOVERY -> UsageUncertainty.PARSE_RECOVERY
    LocalIncompleteness.PARTIAL_EXTRACTION, LocalIncompleteness.DISCARDED_SOURCE_TEXT,
    LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS, LocalIncompleteness.SOURCE_TEXT_UNAVAILABLE -> UsageUncertainty.INCOMPLETE_SOURCE
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
     * (which may not exist for Oracle) overlaps it. For a name, it counts when that text mentions the name, or when it
     * may change the block structure ([LocalRegionCompleteness.reasonsFor]).
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
    val reasons: Set<LocalIncompleteness>,
    /**
     * The reasons that can change what a name lookup through the region finds, a subset of [reasons]: hidden text in
     * the region's own declarative part (not in its statements, nor in a nested region, which declare nothing the
     * region holds), and the reasons that apply to the whole region (recovery, partial extraction, unverified source).
     */
    val lookupReasons: Set<LocalIncompleteness>,
    /** The undecided conditional-compilation pieces behind [LocalIncompleteness.CONDITIONAL_COMPILATION] in [reasons]. */
    private val pieces: List<LocalConditionalPiece> = emptyList(),
    /** The pieces behind [LocalIncompleteness.CONDITIONAL_COMPILATION] in [lookupReasons]. */
    private val lookupPieces: List<LocalConditionalPiece> = emptyList()
) {
    val isComplete: Boolean get() = reasons.isEmpty()

    /**
     * [reasons] as they apply to the declaration or use of [name]: conditional-compilation text counts only when a piece
     * mentions the name (at a position other than [except], the declaration's own name) or may change the block
     * structure. Text that never writes the name cannot declare it, use it or hide it.
     */
    fun reasonsFor(name: OracleIdentifier, except: Long = LocalConditionalPiece.NO_POSITION): Set<LocalIncompleteness> =
        narrowed(reasons, pieces, name, except)

    /** [lookupReasons] as they apply to a lookup of [name]; see [reasonsFor]. */
    fun lookupReasonsFor(name: OracleIdentifier): Set<LocalIncompleteness> =
        narrowed(lookupReasons, lookupPieces, name, LocalConditionalPiece.NO_POSITION)

    /** The pieces behind [LocalIncompleteness.CONDITIONAL_COMPILATION] in [reasons]. */
    fun conditionalPieces(): List<LocalConditionalPiece> = pieces

    /**
     * Conditional compilation when undecided text anywhere in the region, statements included, writes [name] as a
     * label: a label is declared among statements, where the lookup position rule does not look, and it is visible from
     * the block or loop it labels.
     */
    fun labelReasonsFor(name: OracleIdentifier): Set<LocalIncompleteness> =
        if (pieces.any { it.labels(name) }) EnumSet.of(LocalIncompleteness.CONDITIONAL_COMPILATION) else emptySet()

    override fun toString() = if (isComplete) "COMPLETE" else "INCOMPLETE$reasons"

    companion object {
        val COMPLETE = LocalRegionCompleteness(emptySet(), emptySet())

        fun of(
            reasons: Set<LocalIncompleteness>,
            lookupReasons: Set<LocalIncompleteness> = reasons,
            pieces: List<LocalConditionalPiece> = emptyList(),
            lookupPieces: List<LocalConditionalPiece> = pieces
        ): LocalRegionCompleteness =
            if (reasons.isEmpty()) COMPLETE
            else LocalRegionCompleteness(
                Collections.unmodifiableSet(EnumSet.copyOf(reasons)), Collections.unmodifiableSet(enumSet(lookupReasons)),
                pieces.distinct(), lookupPieces.distinct()
            )

        /**
         * [reasons] without conditional compilation when none of [pieces] counts for [name]. With no piece recorded,
         * the reason stays (it is never dropped without the evidence that it does not apply).
         */
        fun narrowed(
            reasons: Set<LocalIncompleteness>, pieces: List<LocalConditionalPiece>, name: OracleIdentifier, except: Long
        ): Set<LocalIncompleteness> {
            if (LocalIncompleteness.CONDITIONAL_COMPILATION !in reasons || pieces.isEmpty() || pieces.any { it.affects(name, except) }) return reasons
            return EnumSet.copyOf(reasons).apply { remove(LocalIncompleteness.CONDITIONAL_COMPILATION) }
        }

        private fun enumSet(reasons: Set<LocalIncompleteness>): Set<LocalIncompleteness> =
            if (reasons.isEmpty()) EnumSet.noneOf(LocalIncompleteness::class.java) else EnumSet.copyOf(reasons)
    }
}

/**
 * Text Oracle may compile differently than the file shows: the gap between two tokens holding a conditional-compilation
 * boundary, a substitution or other discarded text, or the tokens of a kept conditional-compilation branch.
 */
internal class LocalHiddenText(val reason: LocalIncompleteness, val range: SourceRange)

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
    val declarations: List<LocalDeclaration>,
    /** The file's hidden text, in source order. */
    val hiddenText: List<LocalHiddenText> = emptyList(),
    /** Where undecided conditional text, dropped or kept, writes each name, by lookup name. */
    private val conditionalMentions: Map<String, LongArray> = emptyMap()
) {
    /** The positions ([LocalConditionalPiece.position]) where undecided conditional text writes [name]. */
    fun conditionalMentions(name: OracleIdentifier): LongArray = conditionalMentions[name.lookupName] ?: NO_MENTIONS

    /**
     * The reasons of the hidden text a use written at [range] sits under or touches: a kept conditional-compilation
     * branch holding it, or hidden text inside it or right next to it.
     */
    fun hiddenAt(range: SourceRange): Set<LocalIncompleteness> {
        if (hiddenText.isEmpty()) return emptySet()
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        for (text in hiddenText) {
            if (!before(text.range.endLine, text.range.endColumn, range.startLine, range.startColumn) &&
                !before(range.endLine, range.endColumn, text.range.startLine, text.range.startColumn)) reasons += text.reason
        }
        return reasons
    }

    private fun before(line: Int, column: Int, otherLine: Int, otherColumn: Int) = line < otherLine || line == otherLine && column < otherColumn

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

private val NO_MENTIONS = LongArray(0)
