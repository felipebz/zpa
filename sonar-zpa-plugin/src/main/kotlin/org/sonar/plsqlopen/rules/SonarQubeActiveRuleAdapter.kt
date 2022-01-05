/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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

    override fun toString(): String = activeRule.toString()

    override fun equals(other: Any?): Boolean = activeRule == other

    override fun hashCode(): Int = activeRule.hashCode()

}
