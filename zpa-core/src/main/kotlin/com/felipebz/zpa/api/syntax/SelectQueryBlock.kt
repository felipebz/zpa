//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a logical top-level SELECT query block. */
@ZpaExperimentalApi
public interface SelectQueryBlock : SyntaxView {

    /** Raw SELECT_COLUMN nodes belonging directly to this query block. */
    public val selectColumnAstNodes: List<AstNode>

    /** INTO clause for this query block, or null when absent. */
    public val intoClause: SelectIntoClause?

    /** The direct FROM clause, or null when absent. */
    public val fromClauseAstNode: AstNode?

    /** The direct WHERE clause, or null when absent. */
    public val whereClauseAstNode: AstNode?

    /** The direct GROUP BY clause, or null when absent. */
    public val groupByClauseAstNode: AstNode?

    /** The direct HAVING clause, or null when absent. */
    public val havingClauseAstNode: AstNode?

    /** The direct MODEL clause, or null when absent. */
    public val modelClauseAstNode: AstNode?
}
