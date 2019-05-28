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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.sonar.plsqlopen.rules.*
import org.sonar.plugins.plsqlopen.api.ZpaRulesDefinition
import org.sonar.plugins.plsqlopen.api.annotations.Rule
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor

class PlSqlChecksTest {

    private lateinit var activeRules: ActiveRules
    private lateinit var customRulesDefinition: MyCustomPlSqlRulesDefinition
    private val ruleMetadataLoader = RuleMetadataLoader()

    @Before
    fun setUp() {
        val repository = Repository(DEFAULT_REPOSITORY_KEY)
        CustomAnnotationBasedRulesDefinition(repository, "plsqlopen", RuleMetadataLoader())
            .addRuleClasses(false, listOf(MyRule::class.java))

        val customRepository = Repository(CUSTOM_REPOSITORY_KEY)
        customRulesDefinition = MyCustomPlSqlRulesDefinition()
        customRulesDefinition.define(customRepository)

        activeRules = ActiveRules().addRepository(repository).addRepository(customRepository)
    }

    @Test
    fun shouldReturnDefaultChecks() {
        val checks = PlSqlChecks.createPlSqlCheck(activeRules, ruleMetadataLoader)
        checks.addChecks(DEFAULT_REPOSITORY_KEY, listOf(MyRule::class.java))

        val defaultVisitor = visitor(checks, DEFAULT_REPOSITORY_KEY, DEFAULT_RULE_KEY)

        assertThat(checks.all()).hasSize(1)
        assertThat(checks.ruleKey(defaultVisitor)).isNotNull
        assertThat(checks.ruleKey(defaultVisitor)?.rule()).isEqualTo(DEFAULT_RULE_KEY)
        assertThat(checks.ruleKey(defaultVisitor)?.repository()).isEqualTo(DEFAULT_REPOSITORY_KEY)
    }

    @Test
    fun shouldReturnCustomChecks() {
        val checks = PlSqlChecks.createPlSqlCheck(activeRules, ruleMetadataLoader)
        checks.addCustomChecks(arrayOf(customRulesDefinition))

        val customVisitor = visitor(checks, CUSTOM_REPOSITORY_KEY, CUSTOM_RULE_KEY)

        assertThat(checks.all()).hasSize(1)
        assertThat(checks.ruleKey(customVisitor)).isNotNull
        assertThat(checks.ruleKey(customVisitor)?.rule()).isEqualTo(CUSTOM_RULE_KEY)
        assertThat(checks.ruleKey(customVisitor)?.repository()).isEqualTo(CUSTOM_REPOSITORY_KEY)
    }

    @Test
    fun shouldWorkWithoutCustomChecks() {
        val checks = PlSqlChecks.createPlSqlCheck(activeRules, ruleMetadataLoader)
        checks.addCustomChecks(null)
        assertThat(checks.all()).hasSize(0)
    }

    @Test
    fun shouldNotReturnRuleKeyIfCheckDoesNotExists() {
        val checks = PlSqlChecks.createPlSqlCheck(activeRules, ruleMetadataLoader)
        checks.addChecks(DEFAULT_REPOSITORY_KEY, listOf(MyRule::class.java))

        assertThat(checks.ruleKey(MyCustomRule())).isNull()
    }

    private fun visitor(plSqlChecks: PlSqlChecks, repository: String, rule: String): PlSqlVisitor {
        val key = RuleKey(repository, rule)

        var visitor: PlSqlVisitor? = null

        for (checks in plSqlChecks.checks) {
            visitor = checks.of(key)

            if (visitor != null) {
                return visitor
            }
        }
        return visitor ?: fail("Should return a visitor.")
    }

    @Rule(key = DEFAULT_RULE_KEY, name = "This is the default rules", description = "desc")
    class MyRule : PlSqlCheck()

    @Rule(key = CUSTOM_RULE_KEY, name = "This is a custom rules", description = "desc")
    class MyCustomRule : PlSqlCheck()

    class MyCustomPlSqlRulesDefinition : ZpaRulesDefinition {
        fun define(repository: ZpaRepository) {
            CustomAnnotationBasedRulesDefinition(repository, "plsqlopen", RuleMetadataLoader())
                .addRuleClasses(false, checkClasses().toList())
        }

        override fun repositoryName(): String {
            return "Custom Rule Repository"
        }

        override fun repositoryKey(): String {
            return CUSTOM_REPOSITORY_KEY
        }

        override fun checkClasses(): Array<Class<*>> {
            return arrayOf(MyCustomRule::class.java)
        }
    }

    companion object {
        private const val DEFAULT_REPOSITORY_KEY = "DefaultRuleRepository"
        private const val DEFAULT_RULE_KEY = "MyRule"
        private const val CUSTOM_REPOSITORY_KEY = "CustomRuleRepository"
        private const val CUSTOM_RULE_KEY = "MyCustomRule"
    }
}
