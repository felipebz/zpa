//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    CONDITIONAL_DIRECTIVE,

    /**
     * Skipped-text trivia: as [CONDITIONAL_BRANCH_BOUNDARY], for a selection directive the configured settings decide:
     * each condition up to the kept branch is `TRUE`, `FALSE` or `NULL`, and the kept branch is not just an `$ERROR`.
     * The tokens between its opening and its closing boundary are what Oracle compiles with those settings.
     */
    CONDITIONAL_DECIDED_BOUNDARY,

    /**
     * Skipped-text trivia: a whole `$IF … $END` the configured settings decide to keep nothing of (no condition is
     * `TRUE` and there is no `$ELSE`); Oracle compiles none of it with those settings.
     */
    CONDITIONAL_DECIDED_DIRECTIVE;

    override val value = name

    override fun hasToBeSkippedFromAst(node: AstNode?) = false
}
