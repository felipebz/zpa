//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.zpa.api.annotations.RuleCategory
import com.felipebz.zpa.api.checks.PlSqlCheck
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.sonar.api.rules.CleanCodeAttribute
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import com.felipebz.zpa.api.annotations.Rule as ZpaRule

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
        assertThat(repository.rules()).hasSize(2)

        val alertUseRule = repository.rule(RULE_KEY)
        assertThat(alertUseRule).isNotNull
        assertThat(alertUseRule?.name()).isEqualTo(RULE_NAME)

        for (rule in repository.rules()) {
            for (param in rule.params()) {
                assertThat(param.description()).`as`("description for " + param.key()).isNotEmpty
            }
        }

        // org.sonar.check.Rule has no category: SonarQube keeps its default attribute.
        assertThat(alertUseRule?.cleanCodeAttribute()).isNull()

        val zpaRule = checkNotNull(repository.rule(ZPA_RULE_KEY))
        assertThat(zpaRule.cleanCodeAttribute()).isEqualTo(CleanCodeAttribute.IDENTIFIABLE)
    }

    @Test
    fun aDescriptionWithTwoFixSectionsFailsNamingTheRule() {
        val definition = object : CustomPlSqlRulesDefinition() {
            override fun repositoryName() = REPOSITORY_NAME
            override fun repositoryKey() = REPOSITORY_KEY
            override fun checkClasses(): Array<Class<*>> = arrayOf(TwiceSplitRule::class.java)
        }
        assertThatThrownBy { definition.define(RulesDefinition.Context()) }
            .hasMessageContaining("TwiceSplitRule")
    }

    @ZpaRule(key = "TwiceSplitRule", name = "A rule whose description is split twice")
    class TwiceSplitRule : PlSqlCheck()

    @Rule(key = RULE_KEY, name = RULE_NAME, description = "desc", tags = ["bug"])
    inner class MyCustomRule : PlSqlCheck() {
        @RuleProperty(key = "customParam", description = "Custom parameter", defaultValue = "value")
        var customParam = "value"
    }

    @ZpaRule(key = ZPA_RULE_KEY, name = "A rule with ZPA's annotations", description = "desc", category = RuleCategory.NAMING)
    class MyZpaAnnotatedRule : PlSqlCheck()

    class MyCustomPlSqlRulesDefinition : CustomPlSqlRulesDefinition() {
        override fun repositoryName(): String {
            return REPOSITORY_NAME
        }

        override fun repositoryKey(): String {
            return REPOSITORY_KEY
        }

        override fun checkClasses(): Array<Class<*>> {
            return arrayOf(MyCustomRule::class.java, MyZpaAnnotatedRule::class.java)
        }
    }

    companion object {
        private const val REPOSITORY_NAME = "Custom Rule Repository"
        private const val REPOSITORY_KEY = "CustomRuleRepository"

        private const val RULE_NAME = "This is my custom rule"
        private const val RULE_KEY = "MyCustomRule"
        private const val ZPA_RULE_KEY = "MyZpaAnnotatedRule"
    }
}
