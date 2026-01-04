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
package com.felipebz.zpa

import com.felipebz.zpa.rules.RuleMetadataLoader
import com.felipebz.zpa.rules.ZpaActiveRules
import com.felipebz.zpa.rules.ZpaChecks
import com.felipebz.zpa.rules.ZpaRuleKey
import com.felipebz.zpa.api.ZpaRulesDefinition
import com.felipebz.zpa.api.checks.PlSqlVisitor

class PlSqlChecks private constructor(private val activeRules: ZpaActiveRules, private val ruleMetadataLoader: RuleMetadataLoader) {
    private val checksByRepository = hashSetOf<ZpaChecks>()

    val checks: Set<ZpaChecks> = checksByRepository

    fun addChecks(repositoryKey: String, checkClass: Iterable<Class<*>>): PlSqlChecks {
        checksByRepository.add(ZpaChecks(activeRules, repositoryKey, ruleMetadataLoader)
                .addAnnotatedChecks(checkClass))

        return this
    }

    fun addCustomChecks(customRulesDefinitions: Array<ZpaRulesDefinition>?): PlSqlChecks {
        if (customRulesDefinitions != null) {

            for (rulesDefinition in customRulesDefinitions) {
                addChecks(rulesDefinition.repositoryKey(), rulesDefinition.checkClasses().toList())
            }
        }

        return this
    }

    fun all(): List<PlSqlVisitor> {
        val allVisitors = ArrayList<PlSqlVisitor>()

        for (checks in checksByRepository) {
            allVisitors.addAll(checks.all())
        }

        return allVisitors
    }

    fun ruleKey(check: PlSqlVisitor): ZpaRuleKey? {
        var ruleKey: ZpaRuleKey?

        for (checks in checksByRepository) {
            ruleKey = checks.ruleKey(check)

            if (ruleKey != null) {
                return ruleKey
            }
        }
        return null
    }

    companion object {
        fun createPlSqlCheck(activeRules: ZpaActiveRules, ruleMetadataLoader: RuleMetadataLoader): PlSqlChecks {
            return PlSqlChecks(activeRules, ruleMetadataLoader)
        }
    }

}
