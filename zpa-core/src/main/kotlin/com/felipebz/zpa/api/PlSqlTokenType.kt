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
package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.TokenType

enum class PlSqlTokenType : TokenType {
    STRING_LITERAL,
    INTEGER_LITERAL,
    NUMBER_LITERAL,
    DATE_LITERAL,
    TIMESTAMP_LITERAL,
    TIME_LITERAL,
    MLE_INLINE_SOURCE,
    MLE_MODULE_SOURCE,
    WRAPPED_SOURCE,
    GRAPHQL_DUALITY_SOURCE,

    /**
     * Skipped-text trivia: conditional-compilation text that opens or closes the kept branch of a selection
     * directive — `$IF … $THEN` (with any branch skipped before the kept one), `$ELSIF`/`$ELSE … $END` met after the
     * kept branch, or a bare `$END`. The tokens between an opening and a closing boundary may be absent for Oracle.
     */
    CONDITIONAL_BRANCH_BOUNDARY,

    /**
     * Skipped-text trivia: a complete conditional-compilation directive that keeps no text — `$ERROR … $END`, or a
     * whole `$IF … $END` none of whose branches was kept.
     */
    CONDITIONAL_DIRECTIVE;

    override val value = name

    override fun hasToBeSkippedFromAst(node: AstNode?) = false
}
