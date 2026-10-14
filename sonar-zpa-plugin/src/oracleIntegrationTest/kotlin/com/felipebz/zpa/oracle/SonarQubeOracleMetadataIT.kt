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
package com.felipebz.zpa.oracle

import com.sonar.orchestrator.build.SonarScanner
import com.sonar.orchestrator.junit5.OrchestratorExtension
import com.sonar.orchestrator.locator.FileLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.RegisterExtension
import org.sonarqube.ws.client.HttpConnector
import org.sonarqube.ws.client.WsClientFactories
import org.sonarqube.ws.client.ce.TaskRequest
import org.sonarqube.ws.client.issues.SearchRequest
import org.sonarqube.ws.client.settings.ListDefinitionsRequest
import java.io.File
import java.nio.file.Files

/**
 * A real SonarQube server and scanner: the shaded plugin in its SonarQube plugin class loader loads the Oracle JDBC
 * driver from `sonar.zpa.oracle.driverPath`, and a check of a separate, test-only rules plugin (compiled against the
 * relocated API, as third-party rules are) resolves the types through the scanner lifecycle.
 */
@EnabledIfEnvironmentVariable(named = "ZPA_ORACLE_JDBC_URL", matches = ".+")
class SonarQubeOracleMetadataIT {

    private fun scanner(projectKey: String, password: String, vararg properties: Pair<String, String>): SonarScanner =
        SonarScanner.create()
            .setScannerVersion("8.1.0.6389")
            .setProjectDir(projectDir)
            .setProjectKey(projectKey)
            .setProjectName(projectKey)
            .setProjectVersion("1.0")
            .setSourceDirs(".")
            .setProperty("sonar.sourceEncoding", "UTF-8")
            .setProperty("sonar.plsqlopen.file.suffixes", "sql,pks")
            .setProperty(OracleMetadataSettings.URL_KEY, fixture.jdbcUrl)
            .setProperty(OracleMetadataSettings.USER_KEY, fixture.owner.name)
            .setProperty(OracleMetadataSettings.SCHEMAS_KEY, fixture.owner.name)
            .setProperty(OracleMetadataSettings.RELATIONS_KEY, "${fixture.reference.name}.COUNTRIES")
            .setProperty(OracleMetadataSettings.COMPILATION_SCHEMA_KEY, fixture.owner.name)
            .setProperty(OracleMetadataSettings.DRIVER_PATH_KEY, fixture.driverJar.toString())
            .apply { properties.forEach { (k, v) -> setProperty(k, v) } }
            .setEnvironmentVariable("ZPA_ORACLE_PASSWORD", password) as SonarScanner

    private fun provision(projectKey: String) {
        orchestrator.server.provisionProject(projectKey, projectKey)
        orchestrator.server.associateProjectToQualityProfile(projectKey, "plsqlopen", "oracle-probe-profile")
    }

    @Test
    fun aScannerExecutionResolvesOracleTypesInAChecksLifecycle() {
        val key = "oracle-metadata"
        provision(key)
        val before = fixture.columnQueryExecutions(fixture.owner.name)
        val result = orchestrator.executeBuild(scanner(key, fixture.owner.password).setDebugLogs(true))
        assertThat(fixture.columnQueryExecutions(fixture.owner.name) - before).isEqualTo(OracleFixture.COLUMN_QUERIES_PER_ACQUISITION)
        assertThat(result.logs).contains("ZPA Oracle metadata:").doesNotContain(fixture.owner.password)

        val client = WsClientFactories.getDefault().newClient(
            HttpConnector.newBuilder().url(orchestrator.server.url).credentials("admin", "admin").build()
        )
        // The scanner context uploaded to the server (visible to administrators) never carries the password.
        val taskId = Regex("api/ce/task\\?id=([\\w-]+)").find(result.logs)!!.groupValues[1]
        val scannerContext = client.ce().task(TaskRequest().setId(taskId).setAdditionalFields(listOf("scannerContext"))).task.scannerContext
        assertThat(scannerContext).contains(OracleMetadataSettings.URL_KEY).doesNotContain(fixture.owner.password)

        // The parameters reached the scanner without being SonarQube settings: the server offers none of them.
        val definitions = client.settings().listDefinitions(ListDefinitionsRequest()).definitionsList.map { it.key }
        assertThat(definitions).contains("sonar.zpa.concurrentExecution").noneMatch { it.startsWith("sonar.zpa.oracle.") }

        val issues = client.issues().search(SearchRequest().setComponentKeys(listOf(key)).setPs("500")).issuesList
        val probe = issues.filter { it.rule == "zpa-oracle-probe:ExternalTypeProbe" }
            .associate { it.message.substringBefore(' ') to it.message.substringAfter(' ') }
        assertThat(probe).containsAllEntriesOf(
            mapOf(
                "hr_api.v_salary" to "RESOLVED NUMBER(10,2)",
                "hr_api.v_name" to "RESOLVED VARCHAR2(40 CHAR)",
                "hr_api.r_employee" to "RESOLVED (ID NUMBER(6,0), NAME VARCHAR2(40 CHAR), CODE CHAR(3 BYTE), SALARY NUMBER(10,2), HIRED DATE, NOTE CLOB)",
                "gaps.v_synonym" to "RELATION_NOT_FOUND",
                "gaps.v_city" to "INCOMPLETE",
                "otherapp.billing.v_salary" to "INCOMPLETE"
            )
        )
        // Existing rule findings are unaffected.
        assertThat(issues.filter { it.rule == "plsql:EmptyBlock" }.map { it.component }).containsExactly("$key:job.sql")
    }

    @Test
    fun aFailedAcquisitionFailsTheScanWithoutSecrets() {
        val key = "oracle-metadata-denied"
        provision(key)
        val wrong = "Wrong-${fixture.owner.password}"
        val result = orchestrator.executeBuildQuietly(scanner(key, wrong))
        assertThat(result.isSuccess).isFalse()
        assertThat(result.logs).contains("ZPA Oracle metadata: acquisition failed (error 1017)")
            .doesNotContain(wrong).doesNotContain(fixture.owner.password).doesNotContain(fixture.jdbcUrl)
    }

    companion object {
        private lateinit var fixture: OracleFixture
        private val projectDir: File = Files.createTempDirectory("zpa-sonarqube-oracle-it").toFile()

        @JvmField
        @RegisterExtension
        val orchestrator: OrchestratorExtension = OrchestratorExtension.builderEnv()
            .useDefaultAdminCredentialsForBuilds(true)
            .apply {
                val zipFile = System.getProperty("sonar.zipFile")
                if (zipFile != null) setZipFile(File(zipFile)) else setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[26.8]"))
            }
            .setOrchestratorProperty("orchestrator.artifactory.url", "https://repo1.maven.org/maven2")
            .addPlugin(FileLocation.of(File(System.getProperty("zpa.plugin.jar"))))
            .addPlugin(FileLocation.of(File(System.getProperty("zpa.probePlugin.jar"))))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/com/felipebz/zpa/oracle/oracle-probe-profile.xml"))
            .build()

        @JvmStatic
        @BeforeAll
        fun createFixture() {
            fixture = OracleFixture.create()
            fixture.sources().forEach { (name, text) -> projectDir.resolve(name).writeText(text) }
        }

        @JvmStatic
        @AfterAll
        fun dropFixture() {
            projectDir.deleteRecursively()
            if (::fixture.isInitialized) fixture.close()
        }
    }
}
