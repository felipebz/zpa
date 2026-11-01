//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class ActiveRule(
    private val repository: ZpaRepository,
    private val rule: ZpaRule,
    private val configuration: ActiveRuleConfiguration?
) : ZpaActiveRule {

    override val internalKey: String
        get() = rule.key

    override val language: String
        get() = "plsqlopen"

    override fun param(key: String): String = configuration?.parameters?.get(key) ?: ""

    override val params: Map<String, String>
        get() = configuration?.parameters ?: emptyMap()

    override val ruleKey: ZpaRuleKey
        get() = RuleKey(repository.key, rule.key)

    override val severity: String
        get() = configuration?.severity ?: rule.severity

    override val templateRuleKey: String?
        get() = null

    override val tags: Array<String>
        get() = rule.tags

    override val remediationConstant: String
        get() = rule.remediationConstant

    override val name: String
        get() = rule.name

}
