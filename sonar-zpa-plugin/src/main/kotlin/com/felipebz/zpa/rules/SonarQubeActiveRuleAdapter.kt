//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import org.sonar.api.batch.rule.ActiveRule

class SonarQubeActiveRuleAdapter(private val activeRule: ActiveRule) : ZpaActiveRule {

    override val ruleKey: ZpaRuleKey
        get() = SonarQubeRuleKeyAdapter(activeRule.ruleKey())

    override val severity: String
        get() = activeRule.severity()

    override val language: String
        get() = activeRule.language()

    override fun param(key: String): String? = activeRule.param(key)

    override val params: Map<String, String>
        get() = activeRule.params()

    override val internalKey: String?
        get() = activeRule.internalKey()

    override val templateRuleKey: String?
        get() = activeRule.templateRuleKey()

    override val tags: Array<String>
        get() = throw NotImplementedError()

    override val remediationConstant: String
        get() = throw NotImplementedError()

    override val name: String
        get() = throw NotImplementedError()

    override fun toString(): String = activeRule.toString()

    override fun equals(other: Any?): Boolean = other is SonarQubeActiveRuleAdapter && activeRule == other.activeRule

    override fun hashCode(): Int = activeRule.hashCode()

}
