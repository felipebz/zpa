/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plsqlopen

import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonar.api.rule.RuleKey
import org.sonar.plugins.plsqlopen.api.CustomPlSqlRulesDefinition
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import java.util.*

class PlSqlChecks private constructor(private val checkFactory: CheckFactory) {
    private val checksByRepository = hashSetOf<Checks<PlSqlVisitor>>()

    val checks: Set<Checks<PlSqlVisitor>> = checksByRepository

    fun addChecks(repositoryKey: String, checkClass: Iterable<Class<*>>): PlSqlChecks {
        checksByRepository.add(checkFactory
                .create<PlSqlVisitor>(repositoryKey)
                .addAnnotatedChecks(checkClass))

        return this
    }

    fun addCustomChecks(customRulesDefinitions: Array<CustomPlSqlRulesDefinition>?): PlSqlChecks {
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

    fun ruleKey(check: PlSqlVisitor): RuleKey? {
        var ruleKey: RuleKey?

        for (checks in checksByRepository) {
            ruleKey = checks.ruleKey(check)

            if (ruleKey != null) {
                return ruleKey
            }
        }
        return null
    }

    companion object {
        @JvmStatic
        fun createPlSqlCheck(checkFactory: CheckFactory): PlSqlChecks {
            return PlSqlChecks(checkFactory)
        }
    }

}
