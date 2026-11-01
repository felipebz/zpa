//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a PL/SQL SELECT statement. */
@ZpaExperimentalApi
public interface SelectStatement : SyntaxView {

    /** The SELECT_EXPRESSION directly represented by this PL/SQL statement. */
    public val selectExpressionAstNode: AstNode

    /**
     * Logical top-level query blocks in source order.
     *
     * Parenthesized QUERY_BLOCK wrappers are transparently unwrapped. Query
     * blocks belonging to CTEs, scalar subqueries, and inline views are not
     * included.
     */
    public val queryBlocks: List<SelectQueryBlock>

    /** True when the top-level query expression uses a set operator. */
    public val hasSetOperation: Boolean
}
