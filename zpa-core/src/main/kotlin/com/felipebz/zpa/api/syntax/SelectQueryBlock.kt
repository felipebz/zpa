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
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a logical top-level SELECT query block. */
@ZpaExperimentalApi
public interface SelectQueryBlock : SyntaxView {

    /** Raw SELECT_COLUMN nodes belonging directly to this query block. */
    public val selectColumnAstNodes: List<AstNode>

    /** INTO clause for this query block, or null when absent. */
    public val intoClause: SelectIntoClause?

    public val fromClauseAstNode: AstNode?

    public val whereClauseAstNode: AstNode?

    public val groupByClauseAstNode: AstNode?

    public val havingClauseAstNode: AstNode?

    public val modelClauseAstNode: AstNode?
}
