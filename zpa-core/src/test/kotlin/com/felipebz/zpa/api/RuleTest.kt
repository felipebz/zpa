/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
