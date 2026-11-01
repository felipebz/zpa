//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import org.sonar.api.utils.AnnotationUtils

class PlSqlProfile : BuiltInQualityProfilesDefinition {

    private val ruleMetadataLoader = SonarQubeRuleMetadataLoader()

    override fun define(context: BuiltInQualityProfilesDefinition.Context) {
        val profile = context.createBuiltInQualityProfile(CheckList.SONAR_WAY_PROFILE, PlSql.KEY)
        profile.isDefault = true

        for (ruleClass in CheckList.checks) {
            addRule(ruleClass, profile)
        }

        profile.done()
    }

    private fun addRule(ruleClass: Class<*>, profile: BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile) {
        if (AnnotationUtils.getAnnotation(ruleClass, ActivatedByDefault::class.java) != null) {
            val ruleKey = CustomAnnotationBasedRulesDefinition.getRuleKey(ruleMetadataLoader, ruleClass)
            profile.activateRule(PlSqlRuleRepository.KEY, ruleKey)
        }
    }

}
