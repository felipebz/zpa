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
package org.sonar.plsqlopen

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.rules.ExpectedException
import org.sonar.api.server.debt.DebtRemediationFunction.Type
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.api.server.rule.RulesDefinition.*
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonar.plsqlopen.rules.SonarQubeRepositoryAdapter
import org.sonar.plsqlopen.rules.SonarQubeRuleMetadataLoader
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation
import java.util.*

class CustomAnnotationBasedRulesDefinitionTest {

    private val context = Context()

    @get:org.junit.Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Before
    fun setup() {
        Locale.setDefault(Locale.ROOT)
    }

    @Test
    fun noClassToAdd() {
        assertThat(buildRepository().rules()).isEmpty()
    }

    @Test
    fun classWithoutRuleAnnotation() {
        class NotRuleClass
        thrown.expect(IllegalArgumentException::class.java)
        buildSingleRuleRepository(NotRuleClass::class.java)
    }

    @Test
    fun ruleAnnotationData() {
        @Rule(key = "key1", name = "name1", description = "description1", tags = ["mytag"])
        class RuleClass {
            @RuleProperty(key = "param1Key", description = "param1 description")
            var param1 = "x"
        }

        val rule = buildSingleRuleRepository(RuleClass::class.java)
        assertThat(rule.key()).isEqualTo("key1")
        assertThat(rule.name()).isEqualTo("name1")
        assertThat(rule.htmlDescription()).isEqualTo("description1")
        assertThat(rule.markdownDescription()).isNull()
        assertThat(rule.tags()).containsOnly("mytag")
        assertThat(rule.template()).isFalse()
        assertThat(rule.params()).hasSize(1)
        assertParam(rule.params()[0], "param1Key", "param1 description")
    }

    @Rule(name = "name1", description = "description1")
    internal inner class RuleClassWithoutAnnotationDefinedKey

    @Test
    fun ruleWithoutExpliciKey() {
        thrown.expect(IllegalArgumentException::class.java)
        buildSingleRuleRepository(RuleClassWithoutAnnotationDefinedKey::class.java)
    }

    @Test
    fun ruleWithoutExplicitKeyCanBeAcceptable() {
        val repository = buildRepository(LANGUAGE_KEY_WITH_RESOURCE_BUNDLE, false, RuleClassWithoutAnnotationDefinedKey::class.java)
        val rule = repository.rules()[0]
        assertThat(rule.key()).isEqualTo(RuleClassWithoutAnnotationDefinedKey::class.java.canonicalName)
        assertThat(rule.name()).isEqualTo("name1")
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
        assertThat(rule.key()).isEqualTo("ruleWithExternalInfo")
        assertThat(rule.name()).isEqualTo("external name for ruleWithExternalInfo")
        assertThat(rule.htmlDescription()).isEqualTo("description for ruleWithExternalInfo")
        assertThat(rule.params()).hasSize(2)
        assertParam(rule.params()[0], "param1Key", "description for param1")
        assertParam(rule.params()[1], "param2", null)
    }

    @Test
    fun classWithSqaleConstantRemediation() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("10min")
        class RuleClass

        val rule = buildSingleRuleRepository(RuleClass::class.java)
        assertRemediation(rule, Type.CONSTANT_ISSUE, null, "10min", null)
    }

    @Test
    fun invalidSqaleAnnotation() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("xxx")
        class MyInvalidRuleClass

        thrown.expect(IllegalArgumentException::class.java)
        buildSingleRuleRepository(MyInvalidRuleClass::class.java)
    }

    @Test
    fun loadMethodWithClassWithSqaleAnnotations() {
        @Rule(key = "key1", name = "name1", description = "description1")
        @ConstantRemediation("10min")
        class RuleClass

        val repository = load(RuleClass::class.java)
        assertThat(repository.rules()).hasSize(1)
    }

    private fun assertRemediation(rule: RulesDefinition.Rule, type: Type, gapMultiplier: String?, baseEffort: String, gapDescription: String?) {
        val remediationFunction = rule.debtRemediationFunction()
        assertThat(remediationFunction?.type()).isEqualTo(type)
        assertThat(remediationFunction?.gapMultiplier()).isEqualTo(gapMultiplier)
        assertThat(remediationFunction?.baseEffort()).isEqualTo(baseEffort)
        assertThat(rule.gapDescription()).isEqualTo(gapDescription)
    }

    private fun assertParam(param: Param, expectedKey: String, expectedDescription: String?) {
        assertThat(param.key()).isEqualTo(expectedKey)
        assertThat(param.name()).isEqualTo(expectedKey)
        assertThat(param.description()).isEqualTo(expectedDescription)
    }

    private fun buildSingleRuleRepository(ruleClass: Class<*>): RulesDefinition.Rule {
        val repository = buildRepository(ruleClass)
        assertThat(repository.rules()).hasSize(1)
        return repository.rules()[0]
    }

    private fun buildRepository(vararg classes: Class<*>): Repository {
        return buildRepository(LANGUAGE_KEY_WITH_RESOURCE_BUNDLE, true, *classes)
    }

    private fun buildRepository(languageKey: String, failIfNoExplicitKey: Boolean, vararg classes: Class<*>): Repository {
        val newRepository = createRepository(languageKey)
        CustomAnnotationBasedRulesDefinition(SonarQubeRepositoryAdapter(newRepository), languageKey, SonarQubeRuleMetadataLoader())
                .addRuleClasses(failIfNoExplicitKey, classes.toList())
        newRepository.done()
        return context.repository(REPO_KEY) ?: fail("Should build a repository")
    }

    private fun load(vararg classes: Class<*>): Repository {
        val languageKey = LANGUAGE_KEY_WITH_RESOURCE_BUNDLE
        val newRepository = createRepository(languageKey)
        CustomAnnotationBasedRulesDefinition.load(SonarQubeRepositoryAdapter(newRepository), languageKey, classes.toList(), SonarQubeRuleMetadataLoader())
        newRepository.done()
        return context.repository(REPO_KEY) ?: fail("Should build a repository")
    }

    private fun createRepository(languageKey: String): NewRepository {
        return context.createRepository(REPO_KEY, languageKey)
    }

    companion object {
        private const val REPO_KEY = "plsql"
        private const val LANGUAGE_KEY_WITH_RESOURCE_BUNDLE = "languageKey"
    }

}
