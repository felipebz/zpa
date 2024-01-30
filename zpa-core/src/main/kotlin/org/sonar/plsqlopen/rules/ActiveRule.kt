/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
