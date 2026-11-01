//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class Repository(override val key: String) : ZpaRepository {

    private val rules = mutableMapOf<String, ZpaRule>()

    override fun createRule(ruleKey: String): ZpaRule {
        val rule = Rule(ruleKey)
        rules[ruleKey] = rule
        return rule
    }

    override fun rule(ruleKey: String): ZpaRule? = rules[ruleKey]

    val availableRules: List<ZpaRule>
        get() = rules.values.toList()

    override fun toString(): String = key

}
