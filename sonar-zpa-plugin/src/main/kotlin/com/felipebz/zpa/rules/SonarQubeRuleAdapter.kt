//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleCategory
import com.felipebz.zpa.api.annotations.RuleInfo
import org.sonar.api.rule.RuleScope
import org.sonar.api.rules.CleanCodeAttribute
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.api.rule.RuleStatus as SonarQubeRuleStatus

class SonarQubeRuleAdapter(private val newRule: RulesDefinition.NewRule) : ZpaRule {
    override val key: String
        get() = newRule.key()

    override val params: List<ZpaRuleParam>
        get() = newRule.params().map { SonarQubeRuleParamAdapter(it) }

    override var name: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setName(value)
        }

    override var remediationConstant: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().constantPerIssue(value))
        }

    override var scope: RuleInfo.Scope
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setScope(RuleScope.valueOf(value.name))
        }

    override var template: Boolean
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setTemplate(value)
        }

    override var tags: Array<String>
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setTags(*value)
        }

    override var status: RuleStatus
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setStatus(SonarQubeRuleStatus.valueOf(value.name))
        }

    override var severity: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setSeverity(value)
        }

    override var htmlDescription: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setHtmlDescription(value)
        }

    override var isActivatedByDefault: Boolean
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            newRule.setActivatedByDefault(value)
        }

    override var category: RuleCategory
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            // Without a category SonarQube keeps its own default attribute.
            cleanCodeAttribute(value)?.let(newRule::setCleanCodeAttribute)
        }

    override fun createParam(fieldKey: String): ZpaRuleParam =
        SonarQubeRuleParamAdapter(newRule.createParam(fieldKey))
}

internal fun cleanCodeAttribute(category: RuleCategory): CleanCodeAttribute? = when (category) {
    RuleCategory.UNSPECIFIED -> null
    RuleCategory.LOGIC_ERROR -> CleanCodeAttribute.LOGICAL
    RuleCategory.INCOMPLETE -> CleanCodeAttribute.COMPLETE
    RuleCategory.READABILITY -> CleanCodeAttribute.CLEAR
    RuleCategory.CONVENTION -> CleanCodeAttribute.CONVENTIONAL
    RuleCategory.DESIGN -> CleanCodeAttribute.MODULAR
    RuleCategory.PERFORMANCE -> CleanCodeAttribute.EFFICIENT
    RuleCategory.NAMING -> CleanCodeAttribute.IDENTIFIABLE
    RuleCategory.TESTING -> CleanCodeAttribute.TESTED
    RuleCategory.FORMATTING -> CleanCodeAttribute.FORMATTED
    RuleCategory.DUPLICATION -> CleanCodeAttribute.DISTINCT
    RuleCategory.COMPLEXITY -> CleanCodeAttribute.FOCUSED
}
