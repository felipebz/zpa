//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.api.ZpaRulesDefinition
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.rules.RuleMetadataLoader
import com.felipebz.zpa.rules.ZpaActiveRules
import com.felipebz.zpa.rules.ZpaChecks
import com.felipebz.zpa.rules.ZpaRuleKey

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
