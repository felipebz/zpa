//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.TokenType
import com.felipebz.flr.grammar.GrammarRuleKey

internal data class FunctionAlternative(
    val ruleKey: GrammarRuleKey,
    val admissionTokens: List<TokenType>
) {
    constructor(ruleKey: GrammarRuleKey, vararg admissionTokens: TokenType) :
        this(ruleKey, admissionTokens.toList())

    init {
        require(admissionTokens.isNotEmpty()) {
            "Alternative $ruleKey must have at least one admission token."
        }
    }
}
