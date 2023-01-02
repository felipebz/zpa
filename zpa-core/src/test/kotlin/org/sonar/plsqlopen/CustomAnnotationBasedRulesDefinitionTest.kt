/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plsqlopen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonar.plsqlopen.rules.Repository
import org.sonar.plsqlopen.rules.RuleMetadataLoader
import org.sonar.plsqlopen.rules.ZpaRule
import org.sonar.plsqlopen.rules.ZpaRuleParam
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annotations.Rule
import org.sonar.plugins.plsqlopen.api.annotations.RuleProperty
import java.util.*

class CustomAnnotationBasedRulesDefinitionTest {

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.ROOT)
    }

    @Test
    fun noClassToAdd() {
        assertThat(buildRepository().availableRules).isEmpty()
    }

    @Test
    fun classWithoutRuleAnnotation() {
        class NotRuleClass
        assertThrows<IllegalArgumentException> {
            buildSingleRuleRepository(NotRuleClass::class.java)
        }
    }

    @Test
    fun ruleAnnotationData() {
        @Rule(key = "key1", name = "name1", description = "description1", tags = ["mytag"])
        class RuleClass {
            @RuleProperty(key = "param1Key", description = "param1 description")
            var param1 = "x"
        }

        val rule = buildSingleRuleRepository(RuleClass::class.java)
        assertThat(rule.key).isEqualTo("key1")
        assertThat(rule.name).isEqualTo("name1")
        assertThat(rule.htmlDescription).isEqualTo("description1")
        assertThat(rule.tags).containsOnly("mytag")
        assertThat(rule.template).isFalse
        assertThat(rule.params).hasSize(1)
        assertParam(rule.params[0], "param1Key", "param1 description")
    }

    @Rule(name = "name1", description = "description1")
    internal class RuleClassWithoutAnnotationDefinedKey

    @Test
    fun ruleWithoutExplicitKeyCanBeAcceptable() {
        val repository = buildRepository(LANGUAGE_KEY_WITH_RESOURCE_BUNDLE, RuleClassWithoutAnnotationDefinedKey::class.java)
        val rule = repository.availableRules[0]
        assertThat(rule.key).isEqualTo(RuleClassWithoutAnnotationDefinedKey::class.java.simpleName)
        assertThat(rule.name).isEqualTo("name1")
    }

    @Test
    fun externalNamesAndDescriptions() {
        @Rule(key = "ruleWithExternalInfo")
        class RuleClass {
            @RuleProperty(key = "param1Key")
            var param1 = "x"
            @RuleProperty
            var param2 = "x"
        }

        val rule = buildSingleRuleRepository(RuleClass::class.java)
        assertThat(rule.key).isEqualTo("ruleWithExternalInfo")
        assertThat(rule.name).isEqualTo("external name for ruleWithExternalInfo")
        assertThat(rule.htmlDescription).isEqualTo("description for ruleWithExternalInfo")
        assertThat(rule.params).hasSize(2)
        assertParam(rule.params[0], "param1Key", "description for param1")
        assertParam(rule.params[1], "param2", "")
    }

    @Test
    fun classWithSqaleConstantRemediation() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("10min")
        class RuleClass

        val rule = buildSingleRuleRepository(RuleClass::class.java)
        assertThat(rule.remediationConstant).isEqualTo("10min")
    }

    @Test
    fun invalidSqaleAnnotation() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("xxx")
        class MyInvalidRuleClass

        assertThrows<IllegalArgumentException> {
            buildSingleRuleRepository(MyInvalidRuleClass::class.java)
        }
    }

    @Test
    fun loadMethodWithClassWithSqaleAnnotations() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("10min")
        class RuleClass

        val repository = load(RuleClass::class.java)
        assertThat(repository.availableRules).hasSize(1)
    }

    private fun assertParam(param: ZpaRuleParam, expectedKey: String, expectedDescription: String?) {
        assertThat(param.key).isEqualTo(expectedKey)
        assertThat(param.description).isEqualTo(expectedDescription)
    }

    private fun buildSingleRuleRepository(ruleClass: Class<*>): ZpaRule {
        val repository = buildRepository(ruleClass)
        assertThat(repository.availableRules).hasSize(1)
        return repository.availableRules[0]
    }

    private fun buildRepository(vararg classes: Class<*>): Repository {
        return buildRepository(LANGUAGE_KEY_WITH_RESOURCE_BUNDLE, *classes)
    }

    private fun buildRepository(languageKey: String, vararg classes: Class<*>): Repository {
        val newRepository = createRepository()
        CustomAnnotationBasedRulesDefinition(newRepository, languageKey, RuleMetadataLoader())
                .addRuleClasses(classes.toList())
        return newRepository
    }

    private fun load(vararg classes: Class<*>): Repository {
        val languageKey = LANGUAGE_KEY_WITH_RESOURCE_BUNDLE
        val newRepository = createRepository()
        CustomAnnotationBasedRulesDefinition.load(newRepository, languageKey, classes.toList(), RuleMetadataLoader())
        return newRepository
    }

    private fun createRepository(): Repository {
        return Repository(REPO_KEY)
    }

    companion object {
        private const val REPO_KEY = "plsql"
        private const val LANGUAGE_KEY_WITH_RESOURCE_BUNDLE = "languageKey"
    }

}
