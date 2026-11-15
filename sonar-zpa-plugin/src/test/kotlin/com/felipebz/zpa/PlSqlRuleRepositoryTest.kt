//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.sonar.api.issue.impact.Severity
import org.sonar.api.issue.impact.SoftwareQuality
import org.sonar.api.rules.CleanCodeAttribute
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

class PlSqlRuleRepositoryTest {

    private val repository = RulesDefinition.Context().also { PlSqlRuleRepository().define(it) }.repository(PlSqlRuleRepository.KEY)!!

    @Test
    fun aRuleWithTheBugTagHasAReliabilityImpactAndKeepsItsLegacyTypeAndSeverity() {
        val rule = repository.rule("IdenticalExpression")!!
        assertThat(rule.cleanCodeAttribute()).isEqualTo(CleanCodeAttribute.LOGICAL)
        assertThat(rule.defaultImpacts()).containsExactly(entry(SoftwareQuality.RELIABILITY, Severity.BLOCKER))
        assertThat(rule.type()).isEqualTo(RuleType.BUG)
        assertThat(rule.severity()).isEqualTo("BLOCKER")
    }

    @Test
    fun aRuleWithoutTheBugTagHasAMaintainabilityImpact() {
        val rule = repository.rule("VariableName")!!
        assertThat(rule.cleanCodeAttribute()).isEqualTo(CleanCodeAttribute.IDENTIFIABLE)
        assertThat(rule.defaultImpacts()).containsExactly(entry(SoftwareQuality.MAINTAINABILITY, Severity.LOW))
        assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(rule.severity()).isEqualTo("MINOR")
    }

    @Test
    fun theTemplateHasNoCleanCodeAttribute() {
        val rule = repository.rule("XPath")!!
        assertThat(rule.template()).isTrue()
        assertThat(rule.cleanCodeAttribute()).isNull()
        // No explicit impact: the one SonarQube derives from the legacy type and severity (code smell, MAJOR).
        assertThat(rule.defaultImpacts()).containsExactly(entry(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM))
    }
}
