//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange

/*
 * Construction-only state of [LocalSemanticModelBuilder]: mutable drafts of regions and declarations, frozen into the
 * immutable model at the end of one build call. They must not escape that call.
 */

/** Construction-only: the mutable draft of one region. Must not escape a build call. */
internal class RegionDraft(
    val id: LocalRegionId,
    val parent: RegionDraft?,
    val kind: LocalRegionKind,
    val syntax: String,
    val range: SourceRange
) {
    val declarations = ArrayList<LocalDeclarationId>()
    val children = ArrayList<LocalRegionId>()
    val gaps = ArrayList<LocalCoverageGap>()

    /** The first token of the region's statements (`BEGIN …` or a loop's body), as (line, column); null when it has none. */
    var statementsStart: Pair<Int, Int>? = null

    /** Whether the statements start with `BEGIN`: hidden text right before it is then in the declarative part. */
    var statementsOpenWithBegin = false
}

/** Construction-only: the mutable draft of one declaration. Must not escape a build call. */
internal class DeclarationDraft(
    val id: LocalDeclarationId,
    val region: RegionDraft,
    val position: Int,
    val kind: LocalDeclarationKind,
    val name: OracleIdentifier,
    val range: SourceRange,
    val nameRange: SourceRange,
    val type: LocalTypeUse?,
    val isConstant: Boolean,
    val isNotNull: Boolean,
    val hasDefault: Boolean,
    /** Completed while the traversal goes on (body region, labeled region) and when forward links are made. */
    var details: LocalDeclarationDetails?
)

/** Freezes the drafts of one build call into the immutable model. */
internal fun freeze(
    fileId: FileId,
    regions: List<RegionDraft>,
    declarations: List<DeclarationDraft>,
    completeness: List<LocalRegionCompleteness>,
    hiddenText: List<LocalHiddenText>
): LocalSemanticModel {
    val frozenRegions = regions.map { draft ->
        val extraction = if (draft.gaps.isNotEmpty()) LocalExtraction.PARTIAL else LocalExtraction.ALL_ITEMS_EXTRACTED
        LocalRegion(
            draft.id, draft.parent?.id, draft.kind, draft.syntax, draft.range,
            frozenList(draft.declarations), frozenList(draft.children), extraction, frozenList(draft.gaps),
            completeness[draft.id.ordinal]
        )
    }
    val frozenDeclarations = declarations.map {
        LocalDeclaration(
            it.id, it.region.id, it.position, it.kind, it.name, it.range, it.nameRange, it.type,
            it.isConstant, it.isNotNull, it.hasDefault, it.details
        )
    }
    return LocalSemanticModel(fileId, frozenList(frozenRegions), frozenList(frozenDeclarations), frozenList(hiddenText))
}

/** An immutable copy; compact (no wrapper, a shared instance when empty), which matters for many small region lists. */
internal fun <T> frozenList(values: List<T>): List<T> = java.util.List.copyOf(values)
