//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A range of the file: lines from 1, columns from 0, the end exclusive. */
@ZpaExperimentalApi
data class HighlightingRange(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int) {
    override fun toString() = "$startLine:$startColumn-$endLine:$endColumn"
}

/**
 * A declaration of the file and the references of the file that name it, as the local name resolution of the file
 * decides them. An uncertain reference is included when the resolution has a provisional declaration for it. The
 * ranges are valid, no reference overlaps its declaration, no two declarations share a range and no two references of
 * the file share a start.
 */
@ZpaExperimentalApi
class HighlightingPair(val declaration: HighlightingRange, references: List<HighlightingRange>) {
    val references: List<HighlightingRange> = references.toList()

    override fun equals(other: Any?) = other is HighlightingPair && declaration == other.declaration && references == other.references

    override fun hashCode() = 31 * declaration.hashCode() + references.hashCode()

    override fun toString() = "$declaration <- $references"
}
