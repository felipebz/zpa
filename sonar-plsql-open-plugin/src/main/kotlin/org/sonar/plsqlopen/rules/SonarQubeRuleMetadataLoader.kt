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

import org.sonar.plsqlopen.utils.getAnnotation
import org.sonar.plugins.plsqlopen.api.annotations.Priority
import java.lang.reflect.Field
import org.sonar.check.Priority as SonarPriority
import org.sonar.check.Rule as SonarRule
import org.sonar.check.RuleProperty as SonarRuleProperty

class SonarQubeRuleMetadataLoader : RuleMetadataLoader() {

    override fun getRuleAnnotation(annotatedClassOrObject: Any): RuleData? {
        val ruleAnnotation = super.getRuleAnnotation(annotatedClassOrObject)
        if (ruleAnnotation != null) {
            return ruleAnnotation
        }

        val sonarRule = getAnnotation(annotatedClassOrObject, SonarRule::class.java)

        if (sonarRule != null) {
            val priority = when(sonarRule.priority) {
                SonarPriority.BLOCKER -> Priority.BLOCKER
                SonarPriority.CRITICAL -> Priority.CRITICAL
                SonarPriority.MAJOR -> Priority.MAJOR
                SonarPriority.MINOR -> Priority.MINOR
                SonarPriority.INFO -> Priority.INFO
            }
            return RuleData(
                sonarRule.key,
                sonarRule.name,
                sonarRule.description,
                priority,
                sonarRule.tags,
                sonarRule.status)
        }

        return null
    }

    override fun getRulePropertyAnnotation(field: Field): RulePropertyData? {
        val rulePropertyAnnotation = super.getRulePropertyAnnotation(field)
        if (rulePropertyAnnotation != null) {
            return rulePropertyAnnotation
        }

        val sonarRuleProperty = field.getAnnotation(SonarRuleProperty::class.java)
        if (sonarRuleProperty != null) {
            return RulePropertyData(
                sonarRuleProperty.key,
                sonarRuleProperty.description,
                sonarRuleProperty.defaultValue,
                sonarRuleProperty.type)
        }

        return null
    }

}
