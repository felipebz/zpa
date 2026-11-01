//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import org.sonar.api.batch.rule.ActiveRules

class SonarQubeActiveRulesAdapter(private val activeRules: ActiveRules) : ZpaActiveRules {

    override fun findByRepository(repository: String): Collection<ZpaActiveRule> =
        activeRules.findByRepository(repository).map { SonarQubeActiveRuleAdapter(it) }

}
