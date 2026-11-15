//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.it

import com.sonar.orchestrator.http.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.sonarqube.ws.client.issues.SearchRequest
import java.io.File

/** The server applies the `sonar-resolve` comments the plugin reports once both settings are enabled. */
class IssueResolutionIntegrationTest {

    @Test
    fun commentsResolveTheIssuesOnTheirLine() {
        // The server refuses the project setting while the global one is off.
        enable("sonar.issues.issueResolution.global.enabled", null)
        orchestrator.server.provisionProject(PROJECT_KEY, PROJECT_KEY)
        enable("sonar.issues.issueResolution.enabled", PROJECT_KEY)
        orchestrator.server.associateProjectToQualityProfile(PROJECT_KEY, "plsqlopen", "it-profile")

        orchestrator.executeBuild(
            Tests.createSonarScanner()
                .setProjectDir(File("src/integrationTest/resources/projects/resolution/"))
                .setProjectKey(PROJECT_KEY)
                .setProjectName(PROJECT_KEY)
                .setProjectVersion("1.0")
                .setSourceDirs("src")
                .setProperty("sonar.sourceEncoding", "UTF-8")
        )

        val issues = Tests.newWsClient(orchestrator)
            .issues()
            .search(SearchRequest().setComponentKeys(listOf(PROJECT_KEY)))
            .issuesList

        assertThat(issues).extracting("rule", "line", "issueStatus").containsExactlyInAnyOrder(
            tuple("plsql:EmptyBlock", 2, "FALSE_POSITIVE"),
            tuple("plsql:EmptyBlock", 5, "ACCEPTED"),
            tuple("plsql:EmptyBlock", 8, "OPEN")
        )
    }

    private fun enable(key: String, component: String?) {
        val call = orchestrator.server.newHttpCall("/api/settings/set")
            .setMethod(HttpMethod.POST)
            .setAdminCredentials()
            .setParam("key", key)
            .setParam("value", "true")
        if (component != null) {
            call.setParam("component", component)
        }
        call.execute()
    }

    companion object {
        private const val PROJECT_KEY = "resolution"

        @JvmField
        @RegisterExtension
        val orchestrator = Tests.ORCHESTRATOR
    }
}
