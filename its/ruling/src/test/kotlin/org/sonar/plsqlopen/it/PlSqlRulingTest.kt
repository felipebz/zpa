/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.it

import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.build.Build
import com.sonar.orchestrator.build.SonarScanner
import com.sonar.orchestrator.locator.FileLocation
import com.sonar.orchestrator.locator.MavenLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class PlSqlRulingTest {

    private fun initializeFormsBuild(projectId: String, sources: String): SonarScanner {
        val build = initializeBuild(projectId, sources, "forms_rules")
        build.setProperty("sonar.zpa.forms.metadata", "metadata.json")
        build.setProperty("sonar.zpa.file.suffixes", "pcd,fun,pks,pkb,tgg")
        return build
    }

    private fun initializeBuild(projectId: String, sources: String? = null, profileName: String = "rules"): SonarScanner {
        orchestrator.server.provisionProject(projectId, projectId)
        orchestrator.server.associateProjectToQualityProfile(projectId, "plsqlopen", profileName)

        val originalSource = sources ?: projectId

        return SonarScanner.create(FileLocation.of("../sources/$originalSource").file)
                .setProjectKey(projectId)
                .setProjectVersion("1")
                .setLanguage("plsqlopen")
                .setSourceEncoding("UTF-8")
                .setSourceDirs(".")
                .setProperty("dump.old", FileLocation.of("src/test/resources/expected/$projectId").file.absolutePath)
                .setProperty("dump.new", FileLocation.of("target/actual/$projectId").file.absolutePath)
                .setProperty("sonar.zpa.file.suffixes", "sql,typ,pkg,pkb,pks,tab,tps,tpb")
                .setProperty("sonar.zpa.errorRecoveryEnabled", "false")
                .setProperty("sonar.cpd.skip", "true")
                .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m")
    }

    private fun executeBuild(build: Build<*>) {
        val projectId = build.getProperty("sonar.projectKey")
        val litsDifferencesFile = FileLocation.of("target/differences_$projectId").file
        build.setProperty("lits.differences", litsDifferencesFile.absolutePath)

        orchestrator.executeBuild(build)

        val differences = String(Files.readAllBytes(Paths.get(litsDifferencesFile.absolutePath)), StandardCharsets.UTF_8)
        assertThat(differences).isEmpty()
    }

    @Test
    fun alexandria_plsql_utils() {
        val build = initializeBuild("alexandria-plsql-utils")
        executeBuild(build)
    }

    @Test
    fun pljson() {
        val build = initializeBuild("pljson")
        executeBuild(build)
    }

    @Test
    fun utPLSQL2() {
        val build = initializeBuild("utPLSQL2")
        executeBuild(build)
    }

    @Test
    fun utPLSQL3() {
        val build = initializeBuild("utPLSQL3")
        build.setSourceDirs("./source")
        build.setProperty("sonar.coverageReportPaths", "")
        build.setProperty("sonar.testExecutionReportPaths", "")
        executeBuild(build)
    }

    @Test
    fun demo0001() {
        val build = initializeFormsBuild("demo0001", "Doag-Forms-extracted/demo0001/WEBUTIL_DEMO")
        executeBuild(build)
    }

    @Test
    fun demo0002() {
        val build = initializeFormsBuild("demo0002", "Doag-Forms-extracted/demo0002/DEMO0002")
        executeBuild(build)
    }

    @Test
    fun demo0002_2() {
        val build = initializeFormsBuild("demo0002_2", "Doag-Forms-extracted/demo0002/FRW_REF")
        executeBuild(build)
    }

    @Test
    fun demo0003() {
        val build = initializeFormsBuild("demo0003", "Doag-Forms-extracted/demo0003/DEMO0003")
        executeBuild(build)
    }

    @Test
    fun demo0004() {
        val build = initializeFormsBuild("demo0004", "Doag-Forms-extracted/demo0004/RELEASE_LOCKS")
        executeBuild(build)
    }

    @Test
    fun demo0005() {
        val build = initializeFormsBuild("demo0005", "Doag-Forms-extracted/demo0005/TIMEOUTPJC_TEST")
        executeBuild(build)
    }

    @Test
    fun demo0006() {
        val build = initializeFormsBuild("demo0006", "Doag-Forms-extracted/demo0006/TIMEOUT_SYS_CLIENT_IDL")
        executeBuild(build)
    }

    @Test
    fun demo0007() {
        val build = initializeFormsBuild("demo0007", "Doag-Forms-extracted/demo0007/CD_DEMO_EXCEL")
        executeBuild(build)
    }

    @Test
    fun demo0008() {
        val build = initializeFormsBuild("demo0008", "Doag-Forms-extracted/demo0008/LATENCY_TEST")
        executeBuild(build)
    }

    @Test
    fun demo0009() {
        val build = initializeFormsBuild("demo0009", "Doag-Forms-extracted/demo0009/FORMSAPI_WIZARD_2905")
        executeBuild(build)
    }

    @Test
    fun demo0010() {
        val build = initializeFormsBuild("demo0010", "Doag-Forms-extracted/demo0010/CHK_MYFFI_SAMPLE5")
        executeBuild(build)
    }

    @Test
    fun demo0011() {
        val build = initializeFormsBuild("demo0011", "Doag-Forms-extracted/demo0011/WEBUTIL_DEMO")
        executeBuild(build)
    }

    @Test
    fun demo0012() {
        val build = initializeFormsBuild("demo0012", "Doag-Forms-extracted/demo0012/PDFVIEWER")
        executeBuild(build)
    }

    @Test
    fun demo0013() {
        val build = initializeFormsBuild("demo0013", "Doag-Forms-extracted/demo0013/COLOR_SLIDER")
        executeBuild(build)
    }

    @Test
    fun demo0014() {
        val build = initializeFormsBuild("demo0014", "Doag-Forms-extracted/demo0014/ACCORDION")
        executeBuild(build)
    }

    @Test
    fun demo0014_2() {
        val build = initializeFormsBuild("demo0014_2", "Doag-Forms-extracted/demo0014/ACCORDION2")
        executeBuild(build)
    }

    @Test
    fun demo0015() {
        val build = initializeFormsBuild("demo0015", "Doag-Forms-extracted/demo0015/MODERNIZE")
        executeBuild(build)
    }

    @Test
    fun demo0016() {
        val build = initializeFormsBuild("demo0016", "Doag-Forms-extracted/demo0016/CHK_CBOX3")
        executeBuild(build)
    }

    @Test
    fun demo0017() {
        val build = initializeFormsBuild("demo0017", "Doag-Forms-extracted/demo0017/POC_ACCOUNT")
        executeBuild(build)
    }

    @Test
    fun demo0018() {
        val build = initializeFormsBuild("demo0018", "Doag-Forms-extracted/demo0018/TEST")
        executeBuild(build)
    }

    @Test
    fun oracleDatabase19() {
        val project = "oracle-database_19"
        if (!File("../sources/$project").exists())
        {
            OracleDocsExtractor().extract()
        }
        val build = initializeBuild(project)
        executeBuild(build)
    }

    companion object {
        @JvmField
        @ClassRule
        val orchestrator: Orchestrator = Orchestrator.builderEnv()
                .setSonarVersion(System.getProperty("sonar.runtimeVersion", "7.6"))
                .addPlugin(FileLocation.byWildcardMavenFilename(
                        File("../../sonar-plsql-open-plugin/target"),
                        "sonar-plsql-open-plugin-*.jar"))
                .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.8.0.1209"))
                .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile.xml"))
                .restoreProfileAtStartup(FileLocation.of("src/test/resources/forms_profile.xml"))
                .build()
    }

}
