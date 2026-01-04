/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import org.sonar.api.utils.AnnotationUtils
import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import com.felipebz.zpa.api.annotations.ActivatedByDefault

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
