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
package org.sonar.plsqlopen.rules

import org.sonar.api.server.rule.RuleParamType
import org.sonar.plsqlopen.utils.getAnnotation
import org.sonar.plsqlopen.utils.getFields
import org.sonar.plsqlopen.utils.log.Loggers
import java.lang.reflect.Field
import java.util.*

class RulesDefinitionAnnotationLoader {

    fun load(repo: ZpaRepository, vararg annotatedClasses: Class<*>) {
        for (annotatedClass in annotatedClasses) {
            loadRule(repo, annotatedClass)
        }
    }

    internal fun loadRule(repo: ZpaRepository, clazz: Class<*>): ZpaRule? {
        val ruleAnnotation = getAnnotation(clazz, org.sonar.check.Rule::class.java)
        if (ruleAnnotation != null) {
            return loadRule(repo, clazz, ruleAnnotation)
        } else {
            LOG.warn("The class " + clazz.canonicalName + " should be annotated with " + org.sonar.check.Rule::class.java)
            return null
        }
    }

    companion object {

        private val LOG = Loggers.getLogger(RulesDefinitionAnnotationLoader::class.java)

        private val TYPE_FOR_CLASS: Map<Class<*>, RuleParamType>

        init {
            val map = HashMap<Class<*>, RuleParamType>()
            map[Int::class.java] = RuleParamType.INTEGER
            map[Float::class.java] = RuleParamType.FLOAT
            map[Boolean::class.java] = RuleParamType.BOOLEAN
            TYPE_FOR_CLASS = Collections.unmodifiableMap(map)
        }

        private fun loadRule(repo: ZpaRepository, clazz: Class<*>, ruleAnnotation: org.sonar.check.Rule): ZpaRule {
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
            val propertyAnnotation = field.getAnnotation(org.sonar.check.RuleProperty::class.java)
            if (propertyAnnotation != null) {
                val fieldKey = propertyAnnotation.key.ifEmpty { field.name }
                val param = rule.createParam(fieldKey)
                param.description = propertyAnnotation.description
                param.defaultValue = propertyAnnotation.defaultValue
            }
        }
    }
}
