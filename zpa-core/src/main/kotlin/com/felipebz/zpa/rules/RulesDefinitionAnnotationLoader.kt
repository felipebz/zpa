//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.CustomAnnotationBasedRulesDefinition.Companion.convertCheckClassName
import com.felipebz.zpa.utils.getFields
import com.felipebz.zpa.utils.log.Loggers
import java.lang.reflect.Field

class RulesDefinitionAnnotationLoader(private val ruleMetadataLoader: RuleMetadataLoader) {

    fun load(repo: ZpaRepository, vararg annotatedClasses: Class<*>) {
        for (annotatedClass in annotatedClasses) {
            loadRule(repo, annotatedClass)
        }
    }

    private fun loadRule(repo: ZpaRepository, clazz: Class<*>): ZpaRule? {
        val ruleAnnotation = ruleMetadataLoader.getRuleAnnotation(clazz)
        return if (ruleAnnotation != null) {
            loadRule(repo, clazz, ruleAnnotation)
        } else {
            LOG.warn("The class " + clazz.canonicalName + " should be annotated with @Rule")
            null
        }
    }
    private fun loadRule(repo: ZpaRepository, clazz: Class<*>, ruleAnnotation: RuleData): ZpaRule {
        val ruleKey = ruleAnnotation.key.ifEmpty { convertCheckClassName(clazz) }
        val ruleName = ruleAnnotation.name
        val description = ruleAnnotation.description

        val rule = repo.createRule(ruleKey)
        rule.name = ruleName
        rule.htmlDescription = description
        rule.severity = ruleAnnotation.priority.name
        rule.status = RuleStatus.valueOf(ruleAnnotation.status)
        rule.tags = ruleAnnotation.tags

        val fields = getFields(clazz, true)
        for (field in fields) {
            loadParameters(rule, field)
        }

        return rule
    }

    private fun loadParameters(rule: ZpaRule, field: Field) {
        val propertyAnnotation = ruleMetadataLoader.getRulePropertyAnnotation(field)
        if (propertyAnnotation != null) {
            val fieldKey = propertyAnnotation.key.ifEmpty { field.name }
            val param = rule.createParam(fieldKey)
            param.description = propertyAnnotation.description
            param.defaultValue = propertyAnnotation.defaultValue
        }
    }

    companion object {
        private val LOG = Loggers.getLogger(RulesDefinitionAnnotationLoader::class.java)
    }
}
