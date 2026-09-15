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
package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import kotlin.jvm.JvmSynthetic

/**
 * An opaque descriptor used to subscribe to a particular [SyntaxView] kind.
 *
 * Descriptors are supplied by [SyntaxViews]; callers do not need to depend on
 * the parser grammar type behind a descriptor.
 */
@ZpaExperimentalApi
public class SyntaxViewKind<T : SyntaxView> private constructor(
    private val astNodeType: AstNodeType,
    private val factory: (AstNode) -> T
) {

    @JvmSynthetic
    internal fun astNodeType(): AstNodeType = astNodeType

    @JvmSynthetic
    internal fun createView(node: AstNode): T = factory(node)

    internal companion object {
        @JvmSynthetic
        fun <T : SyntaxView> create(
            astNodeType: AstNodeType,
            factory: (AstNode) -> T
        ): SyntaxViewKind<T> = SyntaxViewKind(astNodeType, factory)
    }
}
