/**
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
package org.sonar.plsqlopen.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.sonarqube.ws.Measures
import org.sonarqube.ws.client.measures.ComponentRequest
import java.io.File

class MetricsTest {

    @Test
    fun project_level() {
        // Size
        assertThat(getMeasureAsInteger(PROJECT_KEY, "ncloc")).isEqualTo(22)
        assertThat(getMeasureAsInteger(PROJECT_KEY, "lines")).isEqualTo(23)
        assertThat(getMeasureAsInteger(PROJECT_KEY, "files")).isEqualTo(3)
        assertThat(getMeasureAsInteger(PROJECT_KEY, "statements")).isEqualTo(4)
        // Documentation
        assertThat(getMeasureAsInteger(PROJECT_KEY, "comment_lines")).isEqualTo(1)
        assertThat(getMeasureAsDouble(PROJECT_KEY, "comment_lines_density")).isEqualTo(4.3)
        assertThat(getMeasureAsString(PROJECT_KEY, "public_documented_api_density")).isNull()
        // Duplication
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines")).isZero
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_blocks")).isZero
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_files")).isZero
        assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines_density")).isZero
        // Rules
        assertThat(getMeasureAsDouble(PROJECT_KEY, "violations")).isZero
    }

    @Test
    fun file_level() {
        // Size
        assertThat(getMeasureAsInteger(FILE_NAME, "ncloc")).isEqualTo(7)
        assertThat(getMeasureAsInteger(FILE_NAME, "lines")).isEqualTo(7)
        assertThat(getMeasureAsInteger(FILE_NAME, "statements")).isEqualTo(2)
        // Documentation
        assertThat(getMeasureAsInteger(FILE_NAME, "comment_lines")).isZero
        assertThat(getMeasureAsDouble(FILE_NAME, "comment_lines_density")).isZero
        // Duplication
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_lines")).isZero
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_blocks")).isZero
        assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_files")).isZero
        assertThat(getMeasureAsDouble(FILE_NAME, "duplicated_lines_density")).isZero
        // Rules
        assertThat(getMeasureAsInteger(FILE_NAME, "violations")).isZero
    }

    /* Helper methods */
    private fun getMeasure(componentKey: String, metricKey: String): Measures.Measure? {
        val response = Tests.newWsClient(orchestrator).measures()
            .component(ComponentRequest().setComponent(componentKey)
                .setMetricKeys(listOf(metricKey)))
        val measures = response.component.measuresList
        return if (measures.size == 1) measures[0] else null
    }

    private fun getMeasureAsString(componentKey: String, metricKey: String): String? {
        val measure = getMeasure(componentKey, metricKey)
        return measure?.value
    }

    private fun getMeasureAsInteger(componentKey: String, metricKey: String): Int? {
        val measure = getMeasure(componentKey, metricKey)
        return if (measure == null) null else Integer.parseInt(measure.value)
    }

    private fun getMeasureAsDouble(componentKey: String, metricKey: String): Double? {
        val measure = getMeasure(componentKey, metricKey)
        return if (measure == null) null else java.lang.Double.parseDouble(measure.value)
    }

    companion object {

        @JvmField
        @ClassRule
        var orchestrator = Tests.ORCHESTRATOR

        private const val PROJECT_KEY = "metrics"
        private const val FILE_NAME = "$PROJECT_KEY:src/source1.sql"

        @JvmStatic
        @BeforeClass
        fun init() {
            orchestrator.server.provisionProject(PROJECT_KEY, PROJECT_KEY)
            orchestrator.server.associateProjectToQualityProfile(PROJECT_KEY, "plsqlopen", "empty-profile")

            val build = Tests.createSonarScanner().setProjectDir(File("projects/metrics/"))
                    .setProjectKey(PROJECT_KEY).setProjectName(PROJECT_KEY).setProjectVersion("1.0").setSourceDirs("src")
                    .setProperty("sonar.sourceEncoding", "UTF-8")
            orchestrator.executeBuild(build)
        }

    }
}
