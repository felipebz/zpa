//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
