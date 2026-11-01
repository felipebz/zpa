//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class ActiveRules : ZpaActiveRules {

    private val repositories = mutableListOf<Repository>()
    private val activeRuleConfigurers = mutableListOf<ActiveRuleConfigurer>()

    fun addRepository(repository: Repository): ActiveRules = apply {
        repositories.add(repository)
    }

    fun addRuleConfigurer(filter: ActiveRuleConfigurer): ActiveRules = apply {
        activeRuleConfigurers.add(filter)
    }

    override fun findByRepository(repository: String): Collection<ZpaActiveRule> {
        val repo = this.repositories.first { it.key == repository }
        return repo.availableRules
            .mapNotNull { rule ->
                val activeRuleConfiguration = ActiveRuleConfiguration(repo.key, rule.key)
                if (activeRuleConfigurers.all { it.apply(repo, rule, activeRuleConfiguration) }) {
                    ActiveRule(repo, rule, activeRuleConfiguration)
                } else {
                    null
                }
            }
    }

}
