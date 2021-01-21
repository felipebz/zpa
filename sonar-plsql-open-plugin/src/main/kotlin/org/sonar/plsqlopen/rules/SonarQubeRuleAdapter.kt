/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.rules

import org.sonar.api.rule.RuleScope
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.plsqlopen.SonarQubeUtils
import org.sonar.plugins.plsqlopen.api.annotations.RuleInfo
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
            if (SonarQubeUtils.isIsSQ71OrGreater) {
                newRule.setScope(RuleScope.valueOf(value.name))
            }
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


    override fun createParam(fieldKey: String): ZpaRuleParam =
        SonarQubeRuleParamAdapter(newRule.createParam(fieldKey))
}
