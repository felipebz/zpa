//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import org.sonar.api.rule.RuleKey

class SonarQubeRuleKeyAdapter(val ruleKey: RuleKey) : ZpaRuleKey {

    override val rule: String
        get() = ruleKey.rule()

    override val repository: String
        get() = ruleKey.repository()

    override fun toString(): String = ruleKey.toString()

    override fun equals(other: Any?): Boolean = other is SonarQubeRuleKeyAdapter && ruleKey == other.ruleKey

    override fun hashCode(): Int = ruleKey.hashCode()

    companion object {
        fun of(repository: String, rule: String): ZpaRuleKey =
            SonarQubeRuleKeyAdapter(RuleKey.of(repository, rule))
    }

}
