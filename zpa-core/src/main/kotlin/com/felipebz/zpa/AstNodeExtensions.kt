//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.squid.SemanticAstNode


fun AstNode?.typeIs(type: AstNodeType): Boolean = this?.type == type

fun AstNode?.typeIs(types: Array<out AstNodeType>): Boolean  =
    types.any { it == this?.type }

fun AstNode.asSemantic(): SemanticAstNode = (this as SemanticAstNode)

fun List<AstNode>.asSemantic(): List<SemanticAstNode> =
    this.map { it.asSemantic() }
