//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A view of one argument in a generic [MethodCall]. */
@ZpaExperimentalApi
public interface MethodCallArgument : SyntaxView {

    /** The named-argument identifier, or null for a positional argument. */
    public val name: String?

    /** Whether the argument has a direct DISTINCT modifier. */
    public val isDistinct: Boolean

    /** The existing AST expression represented by this argument. */
    public val expressionAstNode: AstNode
}
