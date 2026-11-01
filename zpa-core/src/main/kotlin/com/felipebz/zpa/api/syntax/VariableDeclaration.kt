//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a [com.felipebz.zpa.api.PlSqlGrammar.VARIABLE_DECLARATION] node. */
@ZpaExperimentalApi
public interface VariableDeclaration : SyntaxView {

    /** The declaration name, preserving its source spelling. */
    public val name: String

    /** The identifier node for the declaration name. */
    public val nameAstNode: AstNode

    /** Whether the declaration has the direct CONSTANT modifier. */
    public val isConstant: Boolean

    /** The datatype node declared for this variable. */
    public val datatypeAstNode: AstNode

    /** The explicit nullability clause, if any. */
    public val nullability: VariableNullability

    /** The initializer syntax, or null when the declaration is not initialized. */
    public val initializerKind: VariableInitializerKind?

    /** The initializer expression node, or null when the declaration is not initialized. */
    public val initializerAstNode: AstNode?

}
