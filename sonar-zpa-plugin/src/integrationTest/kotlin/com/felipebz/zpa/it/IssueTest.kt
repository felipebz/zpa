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
package com.felipebz.zpa.it

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.sonarqube.ws.client.issues.SearchRequest
import java.io.File

class IssueTest {

    @Test
    fun issues() {
        val issues = Tests.newWsClient(orchestrator)
            .issues()
            .search(SearchRequest().setComponentKeys(listOf(PROJECT_KEY)))
            .issuesList

        assertThat(issues).extracting("rule", "component")
                .containsExactlyInAnyOrder(
                        tuple("plsql:EmptyBlock", "$PROJECT_KEY:src/source1.sql"),
                        tuple("my-rules:ForbiddenDmlCheck", "$PROJECT_KEY:src/custom_rule.sql"))
    }

    companion object {
        private const val PROJECT_KEY = "issue"

        @JvmField
        @RegisterExtension
        val orchestrator = Tests.ORCHESTRATOR

        @JvmStatic
        @BeforeAll
        fun init() {
            orchestrator.server.provisionProject(PROJECT_KEY, PROJECT_KEY)
            orchestrator.server.associateProjectToQualityProfile(PROJECT_KEY, "plsqlopen", "it-profile")

            val build = Tests.createSonarScanner()
                .setProjectDir(File("src/integrationTest/resources/projects/metrics/"))
                .setProjectKey(PROJECT_KEY)
                .setProjectName(PROJECT_KEY)
                .setProjectVersion("1.0")
                .setSourceDirs("src")
                .setProperty("sonar.sourceEncoding", "UTF-8")
            orchestrator.executeBuild(build)
        }
    }

}
