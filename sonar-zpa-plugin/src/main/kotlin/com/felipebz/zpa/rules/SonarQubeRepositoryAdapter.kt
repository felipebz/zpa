/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
