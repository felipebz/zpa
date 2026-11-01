//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.grammar.GrammarRuleBuilder

internal fun GrammarRuleBuilder.define(e: Any): GrammarRuleBuilder =
    this.`is`(e)

internal fun GrammarRuleBuilder.define(e: Any, vararg rest: Any): GrammarRuleBuilder =
    this.`is`(e, *rest)

