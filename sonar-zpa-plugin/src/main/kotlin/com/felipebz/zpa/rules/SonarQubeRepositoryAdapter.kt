//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import org.sonar.api.server.rule.RulesDefinition

class SonarQubeRepositoryAdapter(private val repository: RulesDefinition.NewRepository) : ZpaRepository {
    override val key: String = repository.key()

    override fun createRule(ruleKey: String): ZpaRule =
        SonarQubeRuleAdapter(repository.createRule(ruleKey))

    override fun rule(ruleKey: String): ZpaRule? {
        val rule = repository.rule(ruleKey)
        return if (rule != null) SonarQubeRuleAdapter(rule) else null
    }
}
