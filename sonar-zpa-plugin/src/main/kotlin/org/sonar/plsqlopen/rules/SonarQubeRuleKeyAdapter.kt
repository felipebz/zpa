/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import org.sonar.api.rule.RuleKey

class SonarQubeRuleKeyAdapter(val ruleKey: RuleKey) : ZpaRuleKey {

    override val rule: String
        get() = ruleKey.rule()

    override val repository: String
        get() = ruleKey.repository()

    override fun toString(): String = ruleKey.toString()

    override fun equals(other: Any?): Boolean = ruleKey == (other as SonarQubeRuleKeyAdapter).ruleKey

    override fun hashCode(): Int = ruleKey.hashCode()

    companion object {
        fun of(repository: String, rule: String): ZpaRuleKey =
            SonarQubeRuleKeyAdapter(RuleKey.of(repository, rule))
    }

}
