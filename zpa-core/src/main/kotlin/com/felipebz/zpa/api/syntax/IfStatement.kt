//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a [com.felipebz.zpa.api.PlSqlGrammar.IF_STATEMENT] node. */
@ZpaExperimentalApi
public interface IfStatement : SyntaxView {

    /** The condition expression of the initial IF branch. */
    public val conditionAstNode: AstNode

    /** The statements node belonging to the initial IF branch. */
    public val statementsAstNode: AstNode

    /**
     * The direct raw statement nodes in the initial IF branch, without
     * recursively flattening nested statements.
     */
    public val statementAstNodes: List<AstNode>

    /** The ELSIF branches in source order. */
    public val elsifBranches: List<IfElsifBranch>

    /** The ELSE branch, or null when the statement has no ELSE branch. */
    public val elseBranch: IfElseBranch?
}
