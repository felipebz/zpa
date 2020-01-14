/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck

class CustomPlSqlRulesDefinitionTest {

    @Test
    fun test() {
        val rulesDefinition = MyCustomPlSqlRulesDefinition()
        val context = RulesDefinition.Context()
        rulesDefinition.define(context)
        val repository = context.repository(REPOSITORY_KEY)
        checkNotNull(repository)

        assertThat(repository.name()).isEqualTo(REPOSITORY_NAME)
        assertThat(repository.language()).isEqualTo("plsqlopen")
        assertThat(repository.rules()).hasSize(1)

        val alertUseRule = repository.rule(RULE_KEY)
        assertThat(alertUseRule).isNotNull
        assertThat(alertUseRule?.name()).isEqualTo(RULE_NAME)

        for (rule in repository.rules()) {
            for (param in rule.params()) {
                assertThat(param.description()).`as`("description for " + param.key()).isNotEmpty()
            }
        }
    }

    @Rule(key = RULE_KEY, name = RULE_NAME, description = "desc", tags = ["bug"])
    inner class MyCustomRule : PlSqlCheck() {
        @RuleProperty(key = "customParam", description = "Custom parameter", defaultValue = "value")
        var customParam = "value"
    }

    class MyCustomPlSqlRulesDefinition : CustomPlSqlRulesDefinition() {
        override fun repositoryName(): String {
            return REPOSITORY_NAME
        }

        override fun repositoryKey(): String {
            return REPOSITORY_KEY
        }

        override fun checkClasses(): Array<Class<*>> {
            return arrayOf(MyCustomRule::class.java)
        }
    }

    companion object {
        private const val REPOSITORY_NAME = "Custom Rule Repository"
        private const val REPOSITORY_KEY = "CustomRuleRepository"

        private const val RULE_NAME = "This is my custom rule"
        private const val RULE_KEY = "MyCustomRule"
    }
}
