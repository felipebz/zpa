/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    @ClassRule
    public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

    private static final String PROJECT_KEY = "metrics";
    private static final String FILE_NAME = "source1.sql";
    private static Sonar wsClient;

    @BeforeClass
    public static void init() {
        orchestrator.resetData();

        SonarScanner build = Tests.createSonarScanner().setProjectDir(new File("projects/metrics/"))
                .setProjectKey(PROJECT_KEY).setProjectName(PROJECT_KEY).setProjectVersion("1.0").setSourceDirs("src")
                .setProperty("sonar.sourceEncoding", "UTF-8").setProfile("empty-profile");
        orchestrator.executeBuild(build);

        wsClient = orchestrator.getServer().getWsClient();
    }

    @Test
    public void project_level() {
        // Size
        assertThat(getProjectMeasure("ncloc").getIntValue()).isEqualTo(10);
        assertThat(getProjectMeasure("lines").getIntValue()).isEqualTo(11);
        assertThat(getProjectMeasure("files").getIntValue()).isEqualTo(2);
        assertThat(getProjectMeasure("directories").getIntValue()).isEqualTo(1);
        assertThat(getProjectMeasure("statements").getIntValue()).isEqualTo(2);
        // Documentation
        assertThat(getProjectMeasure("comment_lines").getIntValue()).isEqualTo(1);
        assertThat(getProjectMeasure("commented_out_code_lines")).isNull();
        assertThat(getProjectMeasure("comment_lines_density").getValue()).isEqualTo(9.1);
        assertThat(getProjectMeasure("public_documented_api_density")).isNull();
        // Duplication
        assertThat(getProjectMeasure("duplicated_lines").getValue()).isEqualTo(0.0);
        assertThat(getProjectMeasure("duplicated_blocks").getValue()).isEqualTo(0.0);
        assertThat(getProjectMeasure("duplicated_files").getValue()).isEqualTo(0.0);
        assertThat(getProjectMeasure("duplicated_lines_density").getValue()).isEqualTo(0.0);
        // Rules
        assertThat(getProjectMeasure("violations").getValue()).isEqualTo(0.0);
        // Tests
        assertThat(getProjectMeasure("tests")).isNull();
        assertThat(getProjectMeasure("coverage")).isNull();
    }

    @Test
    public void file_level() {
        // Size
        assertThat(getFileMeasure("ncloc").getIntValue()).isEqualTo(3);
        assertThat(getFileMeasure("lines").getIntValue()).isEqualTo(3);
        assertThat(getFileMeasure("statements").getIntValue()).isEqualTo(1);
        // Documentation
        assertThat(getFileMeasure("comment_lines").getIntValue()).isEqualTo(0);
        assertThat(getFileMeasure("commented_out_code_lines")).isNull();
        assertThat(getFileMeasure("comment_lines_density").getValue()).isEqualTo(0.0);
        // Duplication
        assertThat(getFileMeasure("duplicated_lines")).isNull();
        assertThat(getFileMeasure("duplicated_blocks")).isNull();
        assertThat(getFileMeasure("duplicated_files")).isNull();
        assertThat(getFileMeasure("duplicated_lines_density")).isNull();
        // Rules
        assertThat(getFileMeasure("violations")).isNull();
    }

    /* Helper methods */

    private Measure getProjectMeasure(String metricKey) {
        Resource resource = wsClient.find(ResourceQuery.createForMetrics(PROJECT_KEY, metricKey));
        return resource == null ? null : resource.getMeasure(metricKey);
    }

    private Measure getFileMeasure(String metricKey) {
        Resource resource = wsClient.find(ResourceQuery.createForMetrics(keyFor(FILE_NAME), metricKey));
        return resource == null ? null : resource.getMeasure(metricKey);
    }

    private static String keyFor(String s) {
        return PROJECT_KEY
                + (orchestrator.getConfiguration().getSonarVersion().isGreaterThanOrEquals("4.2") ? ":src/" : ":") + s;
    }
}
