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
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.client.measures.ComponentRequest;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;

public class MetricsTest {

    @ClassRule
    public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

    private static final String PROJECT_KEY = "metrics";
    private static final String FILE_NAME = PROJECT_KEY + ":src/source1.sql";

    @BeforeClass
    public static void init() {
        orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
        orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "plsqlopen", "empty-profile");

        SonarScanner build = Tests.createSonarScanner().setProjectDir(new File("projects/metrics/"))
                .setProjectKey(PROJECT_KEY).setProjectName(PROJECT_KEY).setProjectVersion("1.0").setSourceDirs("src")
                .setProperty("sonar.sourceEncoding", "UTF-8");
        orchestrator.executeBuild(build);
    }

    @Test
    public void project_level() {
        // Size
        assertThat(getMeasureAsInteger(PROJECT_KEY, "ncloc")).isEqualTo(22);
        assertThat(getMeasureAsInteger(PROJECT_KEY, "lines")).isEqualTo(23);
        assertThat(getMeasureAsInteger(PROJECT_KEY, "files")).isEqualTo(3);
        assertThat(getMeasureAsInteger(PROJECT_KEY, "statements")).isEqualTo(4);
        // Documentation
        assertThat(getMeasureAsInteger(PROJECT_KEY, "comment_lines")).isEqualTo(1);
        assertThat(getMeasureAsDouble(PROJECT_KEY, "comment_lines_density")).isEqualTo(4.3);
        assertThat(getMeasureAsString(PROJECT_KEY, "public_documented_api_density")).isNull();
        // Duplication
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines")).isZero();
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_blocks")).isZero();
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_files")).isZero();
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines_density")).isZero();
        // Rules
        assertThat(getMeasureAsDouble(PROJECT_KEY, "violations")).isZero();
    }

    @Test
    public void file_level() {
        // Size
        assertThat(getMeasureAsInteger(FILE_NAME, "ncloc")).isEqualTo(7);
        assertThat(getMeasureAsInteger(FILE_NAME, "lines")).isEqualTo(7);
        assertThat(getMeasureAsInteger(FILE_NAME, "statements")).isEqualTo(2);
        // Documentation
        assertThat(getMeasureAsInteger(FILE_NAME, "comment_lines")).isZero();
        assertThat(getMeasureAsDouble(FILE_NAME, "comment_lines_density")).isZero();
        // Duplication
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_lines")).isZero();
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_blocks")).isZero();
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_files")).isZero();
        assertThat(getMeasureAsDouble(FILE_NAME, "duplicated_lines_density")).isZero();
        // Rules
        assertThat(getMeasureAsInteger(FILE_NAME, "violations")).isZero();
    }

    /* Helper methods */
    private static Measures.Measure getMeasure(String componentKey, String metricKey) {
        Measures.ComponentWsResponse response = Tests.newWsClient(orchestrator).measures()
                .component(new ComponentRequest().setComponent(componentKey)
                        .setMetricKeys(Collections.singletonList(metricKey)));
        List<Measures.Measure> measures = response.getComponent().getMeasuresList();
        return measures.size() == 1 ? measures.get(0) : null;
    }

    private static String getMeasureAsString(String componentKey, String metricKey) {
        Measures.Measure measure = getMeasure(componentKey, metricKey);
        return (measure == null) ? null : measure.getValue();
    }

    private static Integer getMeasureAsInteger(String componentKey, String metricKey) {
        Measures.Measure measure = getMeasure(componentKey, metricKey);
        return (measure == null) ? null : Integer.parseInt(measure.getValue());
    }

    private static Double getMeasureAsDouble(String componentKey, String metricKey) {
        Measures.Measure measure = getMeasure(componentKey, metricKey);
        return (measure == null) ? null : Double.parseDouble(measure.getValue());
    }
}
