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
import java.util.*

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
internal fun freeze(fileId: FileId, regions: List<RegionDraft>, declarations: List<DeclarationDraft>, recovered: Boolean): LocalSemanticModel {
    val frozenRegions = regions.map { draft ->
        val extraction = if (draft.gaps.isNotEmpty()) LocalExtraction.PARTIAL else LocalExtraction.ALL_ITEMS_EXTRACTED
        LocalRegion(
            draft.id, draft.parent?.id, draft.kind, draft.syntax, draft.range,
            frozenList(draft.declarations), frozenList(draft.children), extraction, frozenList(draft.gaps)
        )
    }
    val frozenDeclarations = declarations.map {
        LocalDeclaration(
            it.id, it.region.id, it.position, it.kind, it.name, it.range, it.nameRange, it.type,
            it.isConstant, it.isNotNull, it.hasDefault, it.details
        )
    }
    val completeness = if (recovered) LocalCompleteness.PARSE_RECOVERY_OBSERVED else LocalCompleteness.NOT_ESTABLISHED
    return LocalSemanticModel(fileId, frozenList(frozenRegions), frozenList(frozenDeclarations), completeness)
}

internal fun <T> frozenList(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
