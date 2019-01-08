/*
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
package org.sonar.plsqlopen.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.ClassRule;
import org.junit.Test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;

public class PlSqlRulingTest {

    @ClassRule
    public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[6.7]"))
      .addPlugin(FileLocation.byWildcardMavenFilename(
              new File("../../sonar-plsql-open-plugin/target"),
              "sonar-plsql-open-plugin-*.jar"))
      .setOrchestratorProperty("litsVersion", "0.6")
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin","sonar-lits-plugin", "0.6"))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile.xml"))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/forms_profile.xml"))
      .build();

    public SonarScanner initializeBuild(String projectId) {
        return initializeBuild(projectId, "rules");
    }
    public SonarScanner initializeBuild(String projectId, String profileName) {
        orchestrator.getServer().provisionProject(projectId, projectId);
        orchestrator.getServer().associateProjectToQualityProfile(projectId, "plsqlopen", profileName);

        return SonarScanner.create(FileLocation.of("../sources/" + projectId).getFile())
            .setProjectKey(projectId)
            .setProjectVersion("1")
            .setLanguage("plsqlopen")
            .setSourceEncoding("UTF-8")
            .setSourceDirs(".")
            .setProperty("dump.old", FileLocation.of("src/test/resources/expected/" + projectId).getFile().getAbsolutePath())
            .setProperty("dump.new", FileLocation.of("target/actual/" + projectId).getFile().getAbsolutePath())
            .setProperty("sonar.plsql.file.suffixes", "sql,typ,pkg,pkb,pks,tab,tps,tpb,pcd,fun,tgg")
            .setProperty("sonar.plsql.errorRecoveryEnabled", "false")
            .setProperty("sonar.cpd.skip", "true")
            .setProperty("sonar.analysis.mode", "preview")
            .setProperty("sonar.issuesReport.html.enable", "true")
            .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");
    }

    public void executeBuild(Build<?> build) throws Exception {
        String projectId = build.getProperty("sonar.projectKey");
        File litsDifferencesFile = FileLocation.of("target/differences_" + projectId).getFile();
        build.setProperty("lits.differences", litsDifferencesFile.getAbsolutePath());

        orchestrator.executeBuild(build);

        String differences = new String(Files.readAllBytes(Paths.get(litsDifferencesFile.getAbsolutePath())), StandardCharsets.UTF_8);
        assertThat(differences).isEmpty();
    }

    @Test
    public void alexandria_plsql_utils() throws Exception {
        SonarScanner build = initializeBuild("alexandria-plsql-utils");
        executeBuild(build);
    }

    @Test
    public void pljson() throws Exception {
        SonarScanner build = initializeBuild("pljson");
        executeBuild(build);
    }

    @Test
    public void utPLSQL2() throws Exception {
        SonarScanner build = initializeBuild("utPLSQL2");
        executeBuild(build);
    }

    @Test
    public void utPLSQL3() throws Exception {
        SonarScanner build = initializeBuild("utPLSQL3");
        build.setSourceDirs("./source");
        build.setProperty("sonar.coverageReportPaths", null);
        build.setProperty("sonar.testExecutionReportPaths", null);
        executeBuild(build);
    }

    @Test
    public void forms() throws Exception {
        String[] modules = { "demo0001", "demo0002", "demo0002_2", "demo0003", "demo0004",
            "demo0005", "demo0006", "demo0007", "demo0008", "demo0009",
            "demo0010", "demo0011", "demo0012", "demo0013", "demo0014",
            "demo0014_2", "demo0015", "demo0016", "demo0017", "demo0018" };
        SonarScanner build = initializeBuild("Doag-Forms-extracted", "forms_rules");
        build.setSourceDirs("");
        for (String module : modules) {
            build.setProperty(module + ".dump.old", FileLocation.of("src/test/resources/expected/" + module).getFile().getAbsolutePath());
            build.setProperty(module + ".dump.new", FileLocation.of("target/actual/" + module).getFile().getAbsolutePath());
            build.setProperty(module + ".lits.differences", FileLocation.of("target/differences_" + module).getFile().getAbsolutePath());
        }
        executeBuild(build);

        orchestrator.executeBuild(build);

        for (String module : modules) {
            String differences = new String(Files.readAllBytes(Paths.get(FileLocation.of("target/differences_" + module).getFile().getAbsolutePath())), StandardCharsets.UTF_8);
            assertThat(differences).isEmpty();
        }
    }

}
