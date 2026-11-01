//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

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
