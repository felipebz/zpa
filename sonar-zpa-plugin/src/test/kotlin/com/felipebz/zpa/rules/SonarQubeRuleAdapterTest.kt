//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.rules.CleanCodeAttribute
import org.sonar.api.server.rule.RulesDefinition

class SonarQubeRuleAdapterTest {

    @Test
    fun eachCategoryReachesTheRuleAsItsCleanCodeAttribute() {
        val context = RulesDefinition.Context()
        val repository = context.createRepository("someRepository", "plsqlopen")
        for (category in RuleCategory.entries) {
            val rule = SonarQubeRuleAdapter(repository.createRule(category.name))
            rule.name = category.name
            rule.htmlDescription = "desc"
            rule.category = category
        }
        repository.done()

        val attributes = RuleCategory.entries.associateWith { context.repository("someRepository")!!.rule(it.name)!!.cleanCodeAttribute() }
        assertThat(attributes).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                RuleCategory.UNSPECIFIED to null,
                RuleCategory.LOGIC_ERROR to CleanCodeAttribute.LOGICAL,
                RuleCategory.INCOMPLETE to CleanCodeAttribute.COMPLETE,
                RuleCategory.READABILITY to CleanCodeAttribute.CLEAR,
                RuleCategory.CONVENTION to CleanCodeAttribute.CONVENTIONAL,
                RuleCategory.DESIGN to CleanCodeAttribute.MODULAR,
                RuleCategory.PERFORMANCE to CleanCodeAttribute.EFFICIENT,
                RuleCategory.NAMING to CleanCodeAttribute.IDENTIFIABLE,
                RuleCategory.TESTING to CleanCodeAttribute.TESTED,
                RuleCategory.FORMATTING to CleanCodeAttribute.FORMATTED,
                RuleCategory.DUPLICATION to CleanCodeAttribute.DISTINCT,
                RuleCategory.COMPLEXITY to CleanCodeAttribute.FOCUSED
            )
        )
    }
}
