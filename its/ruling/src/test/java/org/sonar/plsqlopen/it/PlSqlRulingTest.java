/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;

public class PlSqlRulingTest {

    @ClassRule
    public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(FileLocation.byWildcardMavenFilename(
              new File("../../sonar-plsql-open-plugin/target"),
              "sonar-plsql-open-plugin-*.jar"))
      .setOrchestratorProperty("litsVersion", "0.6")
      .addPlugin("lits")
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile.xml"))
      .build();

    @Test
    public void test() throws Exception {
      assertTrue(
        "SonarQube 5.6 is the minimum version to generate the issues report",
        orchestrator.getConfiguration().getSonarVersion().isGreaterThanOrEquals("5.6"));
      orchestrator.getServer().provisionProject("project", "project");
      orchestrator.getServer().associateProjectToQualityProfile("project", "plsqlopen", "rules");
      File litsDifferencesFile = FileLocation.of("target/differences").getFile();
      SonarScanner build = SonarScanner.create(FileLocation.of("../sources").getFile())
        .setProjectKey("project")
        .setProjectName("project")
        .setProjectVersion("1")
        .setLanguage("plsqlopen")
        .setSourceEncoding("UTF-8")
        .setSourceDirs(".")
        .setProperty("dump.old", FileLocation.of("src/test/resources/expected").getFile().getAbsolutePath())
        .setProperty("dump.new", FileLocation.of("target/actual").getFile().getAbsolutePath())
        .setProperty("sonar.plsql.file.suffixes", "sql,typ,pkg,pkb,pks,tab")
        .setProperty("sonar.plsql.errorRecoveryEnabled", "false")
        .setProperty("sonar.cpd.skip", "true")
        .setProperty("sonar.analysis.mode", "preview")
        .setProperty("sonar.issuesReport.html.enable", "true")
        .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
        .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");
      orchestrator.executeBuild(build);

      assertThat(Files.toString(litsDifferencesFile, StandardCharsets.UTF_8)).isEmpty();
    }
    
}
