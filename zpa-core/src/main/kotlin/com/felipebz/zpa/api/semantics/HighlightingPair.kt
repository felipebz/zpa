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
