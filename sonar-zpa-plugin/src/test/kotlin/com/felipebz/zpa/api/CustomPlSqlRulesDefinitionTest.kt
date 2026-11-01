//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.zpa.api.checks.PlSqlCheck
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.check.Rule
import org.sonar.check.RuleProperty

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
                assertThat(param.description()).`as`("description for " + param.key()).isNotEmpty
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
