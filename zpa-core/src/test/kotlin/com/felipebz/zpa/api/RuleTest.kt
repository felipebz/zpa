//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.impl.Parser
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets

abstract class RuleTest {
    private var errorRecoveryEnabled: Boolean = false

    lateinit var p: Parser<Grammar>

    protected fun setRootRule(ruleKey: GrammarRuleKey) {
        p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, errorRecoveryEnabled))
        p.setRootRule(p.grammar.rule(ruleKey))
    }

    protected fun setErrorRecoveryEnabled(value: Boolean) {
        errorRecoveryEnabled = value
    }
}
