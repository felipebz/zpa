//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.squid.SemanticFileNode

/**
 * A copy of this tree in which [target] has [children] instead of its own, to build trees the parser does not produce.
 * The nodes from this one down to [target] are copied; every other subtree is moved under its copied parent, so the
 * original tree must not be used afterwards.
 */
internal fun AstNode.withChildren(target: AstNode, children: List<AstNode>): AstNode {
    val copy = if (this is SemanticFileNode) SemanticFileNode(type, name, tokenOrNull) else SemanticAstNode(type, name, tokenOrNull)
    val newChildren = if (this === target) {
        children
    } else {
        this.children.map { if (it === target || target.descendsFrom(it)) it.withChildren(target, children) else it }
    }
    newChildren.forEach(copy::addChild)
    return copy
}

private fun AstNode.descendsFrom(ancestor: AstNode): Boolean =
    generateSequence(parentOrNull) { it.parentOrNull }.any { it === ancestor }
