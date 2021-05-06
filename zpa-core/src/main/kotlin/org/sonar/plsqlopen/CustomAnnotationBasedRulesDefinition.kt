/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import org.sonar.plsqlopen.rules.RuleMetadataLoader
import org.sonar.plsqlopen.rules.RulesDefinitionAnnotationLoader
import org.sonar.plsqlopen.rules.ZpaRepository
import org.sonar.plsqlopen.rules.ZpaRule
import org.sonar.plsqlopen.utils.getAnnotation
import org.sonar.plugins.plsqlopen.api.annotations.ActivatedByDefault
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.annotations.RuleTemplate
import java.io.IOException
import java.net.URL
import java.util.*
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation as OldConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo as OldRuleInfo

class CustomAnnotationBasedRulesDefinition(private val repository: ZpaRepository,
                                           private val languageKey: String,
                                           private val ruleMetadataLoader: RuleMetadataLoader) {
    private val locale: Locale = Locale.getDefault()
    private val externalDescriptionBasePath: String = String.format("%s/rules/plsql",
        getLocalizedFolderName(String.format("/org/sonar/l10n/%s", languageKey), locale))

    fun addRuleClasses(failIfNoExplicitKey: Boolean, ruleClasses: Iterable<Class<*>>) {
        val loader = RulesDefinitionAnnotationLoader(ruleMetadataLoader)
        for (annotatedClass in ruleClasses) {
            loader.load(repository, annotatedClass)
        }

        val newRules = ArrayList<ZpaRule>()
        for (ruleClass in ruleClasses) {
            val rule = newRule(ruleClass, failIfNoExplicitKey)
            addHtmlDescription(rule)
            rule.template = getAnnotation(ruleClass, RuleTemplate::class.java) != null
            try {
                setupSqaleModel(rule, ruleClass)
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("Could not setup SQALE model on $ruleClass", e)
            }

            val oldRuleInfo = getAnnotation(ruleClass, OldRuleInfo::class.java)
            if (oldRuleInfo != null) {
                rule.scope = RuleInfo.Scope.valueOf(oldRuleInfo.scope.name)
            }

            // TODO: remove this code in the next release
            val ruleInfo = getAnnotation(ruleClass, RuleInfo::class.java)
            if (ruleInfo != null) {
                rule.scope = ruleInfo.scope
            }

            val activatedByDefault = getAnnotation(ruleClass, ActivatedByDefault::class.java)
            if (activatedByDefault != null) {
                rule.isActivatedByDefault = true
            }

            newRules.add(rule)
        }
        setupExternalNames(newRules)
    }

    private fun addHtmlDescription(rule: ZpaRule) {
        val resource = CustomAnnotationBasedRulesDefinition::class.java.getResource("$externalDescriptionBasePath/${rule.key}.html")
        if (resource != null) {
            addHtmlDescription(rule, resource)
        }
    }

    private fun addHtmlDescription(rule: ZpaRule, resource: URL) {
        try {
            rule.htmlDescription = resource.readText()
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read: $resource", e)
        }

    }

    private fun newRule(ruleClass: Class<*>, failIfNoExplicitKey: Boolean): ZpaRule {
        val ruleAnnotation = ruleMetadataLoader.getRuleAnnotation(ruleClass)
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

    private fun setupExternalNames(rules: Collection<ZpaRule>) {
        val bundle: ResourceBundle
        try {
            bundle = ResourceBundle.getBundle("org.sonar.l10n.$languageKey", locale)
        } catch (e: MissingResourceException) {
            return
        }

        for (rule in rules) {
            val baseKey = rule.key
            val nameKey = "$baseKey.name"
            if (bundle.containsKey(nameKey)) {
                rule.name = bundle.getString(nameKey)
            }
            for (param in rule.params) {
                val paramDescriptionKey = baseKey + ".param." + param.key
                if (bundle.containsKey(paramDescriptionKey)) {
                    param.description = bundle.getString(paramDescriptionKey)
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
        fun load(repository: ZpaRepository, languageKey: String, ruleClasses: Iterable<Class<*>>, ruleMetadataLoader: RuleMetadataLoader) {
            CustomAnnotationBasedRulesDefinition(repository, languageKey, ruleMetadataLoader).addRuleClasses(true, ruleClasses)
        }

        private fun setupSqaleModel(rule: ZpaRule, ruleClass: Class<*>) {
            // TODO: remove this code in the next release
            val oldConstant = getAnnotation(ruleClass, OldConstantRemediation::class.java)
            if (oldConstant != null) {
                rule.remediationConstant = oldConstant.value
            }

            val constant = getAnnotation(ruleClass, ConstantRemediation::class.java)
            if (constant != null) {
                rule.remediationConstant = constant.value
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
