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

import org.sonar.api.rule.RuleScope
import org.sonar.api.server.rule.RulesDefinition.NewRepository
import org.sonar.api.server.rule.RulesDefinition.NewRule
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader
import org.sonar.api.utils.AnnotationUtils
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.annnotations.RuleTemplate
import java.io.IOException
import java.net.URL
import java.util.*

class CustomAnnotationBasedRulesDefinition(private val repository: NewRepository, private val languageKey: String) {
    private val locale: Locale = Locale.getDefault()
    private val externalDescriptionBasePath: String

    init {
        this.externalDescriptionBasePath = String.format("%s/rules/plsql",
                getLocalizedFolderName(String.format("/org/sonar/l10n/%s", languageKey), locale))

    }

    fun addRuleClasses(failIfNoExplicitKey: Boolean, ruleClasses: Iterable<Class<*>>) {
        val loader = RulesDefinitionAnnotationLoader()
        for (annotatedClass in ruleClasses) {
            loader.load(repository, annotatedClass)
        }

        val newRules = ArrayList<NewRule>()
        for (ruleClass in ruleClasses) {
            val rule = newRule(ruleClass, failIfNoExplicitKey)
            addHtmlDescription(rule)
            rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, RuleTemplate::class.java) != null)
            try {
                setupSqaleModel(rule, ruleClass)
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("Could not setup SQALE model on $ruleClass", e)
            }

            if (SonarQubeUtils.isIsSQ71OrGreater) {
                val ruleInfo = AnnotationUtils.getAnnotation(ruleClass, RuleInfo::class.java)
                if (ruleInfo != null) {
                    var scope = RuleScope.defaultScope()
                    when {
                        ruleInfo.scope === RuleInfo.Scope.ALL -> scope = RuleScope.ALL
                        ruleInfo.scope === RuleInfo.Scope.MAIN -> scope = RuleScope.MAIN
                        ruleInfo.scope === RuleInfo.Scope.TEST -> scope = RuleScope.TEST
                    }
                    rule.setScope(scope)
                }
            }

            newRules.add(rule)
        }
        setupExternalNames(newRules)
    }

    private fun addHtmlDescription(rule: NewRule) {
        val resource = CustomAnnotationBasedRulesDefinition::class.java.getResource(externalDescriptionBasePath + "/" + rule.key() + ".html")
        if (resource != null) {
            addHtmlDescription(rule, resource)
        }
    }

    private fun addHtmlDescription(rule: NewRule, resource: URL) {
        try {
            rule.setHtmlDescription(resource.readText())
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read: $resource", e)
        }

    }

    private fun newRule(ruleClass: Class<*>, failIfNoExplicitKey: Boolean): NewRule {
        val ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule::class.java)
                ?: throw IllegalArgumentException("No Rule annotation was found on $ruleClass")
        var ruleKey = ruleAnnotation.key
        if (ruleKey.isEmpty()) {
            if (failIfNoExplicitKey) {
                throw IllegalArgumentException("No key is defined in Rule annotation of $ruleClass")
            }
            ruleKey = ruleClass.canonicalName
        }
        return repository.rule(ruleKey) ?: throw IllegalStateException("Rule $ruleKey was not created")
    }

    private fun setupExternalNames(rules: Collection<NewRule>) {
        val bundle: ResourceBundle
        try {
            bundle = ResourceBundle.getBundle("org.sonar.l10n.$languageKey", locale)
        } catch (e: MissingResourceException) {
            return
        }

        for (rule in rules) {
            val baseKey = rule.key()
            val nameKey = "$baseKey.name"
            if (bundle.containsKey(nameKey)) {
                rule.setName(bundle.getString(nameKey))
            }
            for (param in rule.params()) {
                val paramDescriptionKey = baseKey + ".param." + param.key()
                if (bundle.containsKey(paramDescriptionKey)) {
                    param.setDescription(bundle.getString(paramDescriptionKey))
                }
            }
        }
    }

    companion object {
        /**
         * Adds annotated rule classes to an instance of NewRepository. Fails if one of
         * the classes has no SQALE annotation.
         * @param repository repository of rules
         * @param languageKey language identifier
         * @param ruleClasses classes to add
         */
        fun load(repository: NewRepository, languageKey: String, ruleClasses: Iterable<Class<*>>) {
            CustomAnnotationBasedRulesDefinition(repository, languageKey).addRuleClasses(true, ruleClasses)
        }

        private fun setupSqaleModel(rule: NewRule, ruleClass: Class<*>) {
            val constant = AnnotationUtils.getAnnotation(ruleClass, ConstantRemediation::class.java)

            if (constant != null) {
                rule.setDebtRemediationFunction(rule.debtRemediationFunctions().constantPerIssue(constant.value))
            }
        }

        fun getLocalizedFolderName(baseName: String, locale: Locale): String {
            val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)

            var path = control.toBundleName(baseName, locale)
            var url: URL? = CustomAnnotationBasedRulesDefinition::class.java.getResource(path)

            if (url == null) {
                val localeWithoutCountry = if (locale.country == null) locale else Locale(locale.language)
                path = control.toBundleName(baseName, localeWithoutCountry)
                url = CustomAnnotationBasedRulesDefinition::class.java.getResource(path)

                if (url == null) {
                    path = baseName
                    CustomAnnotationBasedRulesDefinition::class.java.getResource(path)
                }
            }

            return path
        }
    }

}
