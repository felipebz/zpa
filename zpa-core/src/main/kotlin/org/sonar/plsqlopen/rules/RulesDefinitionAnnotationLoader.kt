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
package org.sonar.plsqlopen.rules

import org.sonar.plsqlopen.utils.getFields
import org.sonar.plsqlopen.utils.log.Loggers
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
        val ruleKey = ruleAnnotation.key.ifEmpty { clazz.canonicalName }
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
