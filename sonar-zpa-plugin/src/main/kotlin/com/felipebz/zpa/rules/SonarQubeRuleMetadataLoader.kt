//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.utils.getAnnotation
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
