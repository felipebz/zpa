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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueTest {

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  private static final String PROJECT_KEY = "metrics";
  private static final String FILE_NAME = PROJECT_KEY + ":src/source1.sql";


  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    SonarScanner build = Tests.createSonarScanner()
      .setProjectDir(new File("projects/metrics/"))
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_KEY)
      .setProjectVersion("1.0")
      .setSourceDirs("src")
      .setProperty("sonar.sourceEncoding", "UTF-8")
      .setProfile("it-profile");
    orchestrator.executeBuild(build);
  }

  @Test
  public void one_issue() {
    List<Issue> issues = getIssues(PROJECT_KEY);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getRule()).isEqualTo("plsql:EmptyBlock");
    assertThat(issues.get(0).getComponent()).isEqualTo(FILE_NAME);
  }

  /* Helper methods */
  private List<Issue> getIssues(String componentKey) {
      return Tests.newWsClient(orchestrator)
              .issues()
              .search(new SearchWsRequest().setComponentKeys(Collections.singletonList(componentKey)))
              .getIssuesList();
  }
}
