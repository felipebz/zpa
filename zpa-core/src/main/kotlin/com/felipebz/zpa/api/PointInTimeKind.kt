//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

/**
 * The `SCN` or `TIMESTAMP` keyword written before a point-in-time expression (`AS OF TIMESTAMP …`, `TO TIMESTAMP …`).
 * `TIMESTAMP 'text'` lexes as one timestamp literal: that literal starts the expression and stands for the keyword and
 * its string, so a rule reads `pointInTimeKind(b)` followed by the expression.
 */
internal fun pointInTimeKind(b: PlSqlGrammarBuilder): Any =
    b.firstOf(PlSqlKeyword.SCN, PlSqlKeyword.TIMESTAMP, b.next(PlSqlTokenType.TIMESTAMP_LITERAL))
