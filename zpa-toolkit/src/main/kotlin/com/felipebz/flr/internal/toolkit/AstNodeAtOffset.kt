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
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode

/** The node to inspect at [offset]: nodes are in breadth-first order; ties keep the first (shallowest) node. */
internal fun astNodeAtOffset(nodes: Sequence<AstNode>, offset: Int, lineOffsets: LineOffsets): AstNode? {
    // The caller's sequence may be single-use (an Enumeration), and it is scanned twice.
    val tokenNodes = nodes.filter { it.hasToken() }.toList()
    val containingStart = tokenNodes.firstNotNullOfOrNull {
        val start = lineOffsets.getStartOffset(it.token)
        if (offset >= start && offset < lineOffsets.getEndOffset(it.token)) start else null
    }
    val anchor = containingStart ?: offset
    var nearest: AstNode? = null
    var nearestStart = Int.MAX_VALUE
    for (node in tokenNodes) {
        val start = lineOffsets.getStartOffset(node.token)
        if (start in anchor until nearestStart) {
            nearestStart = start
            nearest = node
        }
    }
    return nearest
}
