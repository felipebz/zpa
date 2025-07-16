/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa

import com.felipebz.zpa.rules.RuleMetadataLoader
import com.felipebz.zpa.rules.RulesDefinitionAnnotationLoader
import com.felipebz.zpa.rules.ZpaRepository
import com.felipebz.zpa.rules.ZpaRule
import com.felipebz.zpa.utils.getAnnotation
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.RuleTemplate
import java.io.IOException
import java.net.URL
import java.util.*

class CustomAnnotationBasedRulesDefinition(private val repository: ZpaRepository,
                                           private val languageKey: String,
                                           private val ruleMetadataLoader: RuleMetadataLoader) {
    private val locale: Locale = Locale.getDefault()
    private val externalDescriptionBasePath: String = String.format("%s/rules/plsql",
        getLocalizedFolderName(String.format("/org/sonar/l10n/%s", languageKey), locale))

    fun addRuleClasses(ruleClasses: Iterable<Class<*>>) {
        val loader = RulesDefinitionAnnotationLoader(ruleMetadataLoader)
        for (annotatedClass in ruleClasses) {
            loader.load(repository, annotatedClass)
        }

        val newRules = ArrayList<ZpaRule>()
        for (ruleClass in ruleClasses) {
            val rule = newRule(ruleClass)
            addHtmlDescription(rule)
            rule.template = getAnnotation(ruleClass, RuleTemplate::class.java) != null

            try {
                val constant = getAnnotation(ruleClass, ConstantRemediation::class.java)
                if (constant != null) {
                    rule.remediationConstant = constant.value
                }
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("Invalid remediation constant on $ruleClass", e)
            }

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

    private fun newRule(ruleClass: Class<*>): ZpaRule {
        val ruleKey = getRuleKey(ruleMetadataLoader, ruleClass)
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
            CustomAnnotationBasedRulesDefinition(repository, languageKey, ruleMetadataLoader).addRuleClasses(ruleClasses)
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

        fun convertCheckClassName(ruleClass: Class<*>): String {
            var name = ruleClass.simpleName
            if (name.endsWith("Check")) {
                name = name.substring(0, name.length - 5)
            }
            return name
        }

        fun getRuleKey(ruleMetadataLoader: RuleMetadataLoader, ruleClass: Class<*>): String {
            val ruleAnnotation = ruleMetadataLoader.getRuleAnnotation(ruleClass)
                ?: throw IllegalArgumentException("No Rule annotation was found on $ruleClass")
            return ruleAnnotation.key.ifEmpty { convertCheckClassName(ruleClass) }
        }
    }

}
