//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of one ELSIF branch in an [IfStatement]. */
@ZpaExperimentalApi
public interface IfElsifBranch : SyntaxView {

    /** The ELSIF condition expression. */
    public val conditionAstNode: AstNode

    /** The statements node belonging to this branch. */
    public val statementsAstNode: AstNode

    /** The direct raw statement nodes in this branch, without recursively flattening nested statements. */
    public val statementAstNodes: List<AstNode>
}
