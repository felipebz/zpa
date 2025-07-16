/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.sonarqube.ws.Measures
import org.sonarqube.ws.client.measures.ComponentRequest
import java.io.File

class MetricsTest {

    @Test
    fun project_level() {
        // Size
        assertAll(
            // Size
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "ncloc")).isEqualTo(32) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "lines")).isEqualTo(33) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "files")).isEqualTo(4) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "statements")).isEqualTo(7) },
            // Documentation
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "comment_lines")).isEqualTo(1) },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "comment_lines_density")).isEqualTo(3.0) },
            { assertThat(getMeasureAsString(PROJECT_KEY, "public_documented_api_density")).isNull() },
            // Duplication
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines")).isZero },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_blocks")).isZero },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_files")).isZero },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "duplicated_lines_density")).isZero },
            // Rules
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "violations")).isZero },
            // Tests
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "tests")).isEqualTo(6) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "skipped_tests")).isEqualTo(1) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "test_failures")).isEqualTo(1) },
            { assertThat(getMeasureAsInteger(PROJECT_KEY, "test_errors")).isEqualTo(1) },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "test_success_density")).isEqualTo(66.7) },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "test_execution_time")).isEqualTo(15.0) },
            // Coverage
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "coverage")).isEqualTo(50.0) },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "line_coverage")).isEqualTo(50.0) },
            { assertThat(getMeasureAsDouble(PROJECT_KEY, "branch_coverage")).isNull() },
        )
    }

    @Test
    fun file_level() {
        assertAll(
            // Size
            { assertThat(getMeasureAsInteger(FILE_NAME, "ncloc")).isEqualTo(7) },
            { assertThat(getMeasureAsInteger(FILE_NAME, "lines")).isEqualTo(7) },
            { assertThat(getMeasureAsInteger(FILE_NAME, "statements")).isEqualTo(2) },
            // Documentation
            { assertThat(getMeasureAsInteger(FILE_NAME, "comment_lines")).isZero },
            { assertThat(getMeasureAsDouble(FILE_NAME, "comment_lines_density")).isZero },
            // Duplication
            { assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_lines")).isZero },
            { assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_blocks")).isZero },
            { assertThat(getMeasureAsInteger(FILE_NAME, "duplicated_files")).isZero },
            { assertThat(getMeasureAsDouble(FILE_NAME, "duplicated_lines_density")).isZero },
            // Rules
            { assertThat(getMeasureAsInteger(FILE_NAME, "violations")).isZero },
        )
    }

    @Test
    fun covered_file() {
        assertAll(
            // Size
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "ncloc")).isEqualTo(10) },
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "lines")).isEqualTo(10) },
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "statements")).isEqualTo(3) },
            // Documentation
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "comment_lines")).isZero },
            { assertThat(getMeasureAsDouble(COVERED_FILE_NAME, "comment_lines_density")).isZero },
            // Duplication
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "duplicated_lines")).isZero },
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "duplicated_blocks")).isZero },
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "duplicated_files")).isZero },
            { assertThat(getMeasureAsDouble(COVERED_FILE_NAME, "duplicated_lines_density")).isZero },
            // Rules
            { assertThat(getMeasureAsInteger(COVERED_FILE_NAME, "violations")).isZero },
            // Coverage
            { assertThat(getMeasureAsDouble(COVERED_FILE_NAME, "coverage")).isEqualTo(100.0) },
            { assertThat(getMeasureAsDouble(COVERED_FILE_NAME, "line_coverage")).isEqualTo(100.0) },
            { assertThat(getMeasureAsDouble(COVERED_FILE_NAME, "branch_coverage")).isNull() },
        )
    }

    @Test
    fun test_file() {
        assertAll(
            // Size
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "ncloc")).isNull() },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "lines")).isNull() },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "statements")).isNull() },
            // Documentation
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "comment_lines")).isNull() },
            { assertThat(getMeasureAsDouble(TEST_FILE_NAME, "comment_lines_density")).isNull() },
            // Duplication
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "duplicated_lines")).isZero },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "duplicated_blocks")).isZero },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "duplicated_files")).isZero },
            { assertThat(getMeasureAsDouble(TEST_FILE_NAME, "duplicated_lines_density")).isZero },
            // Rules
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "violations")).isZero },
            // Tests
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "tests")).isEqualTo(6) },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "skipped_tests")).isEqualTo(1) },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "test_failures")).isEqualTo(1) },
            { assertThat(getMeasureAsInteger(TEST_FILE_NAME, "test_errors")).isEqualTo(1) },
            { assertThat(getMeasureAsDouble(TEST_FILE_NAME, "test_success_density")).isEqualTo(66.7) },
            { assertThat(getMeasureAsDouble(TEST_FILE_NAME, "test_execution_time")).isEqualTo(15.0) },
        )
    }

    /* Helper methods */
    private fun getMeasure(componentKey: String, metricKey: String): Measures.Measure? {
        val response = Tests.newWsClient(orchestrator).measures()
            .component(
                ComponentRequest()
                    .setComponent(componentKey)
                    .setMetricKeys(listOf(metricKey))
            )
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
        @RegisterExtension
        var orchestrator = Tests.ORCHESTRATOR

        private const val PROJECT_KEY = "metrics"
        private const val FILE_NAME = "$PROJECT_KEY:src/source1.sql"
        private const val COVERED_FILE_NAME = "$PROJECT_KEY:src/betwnstr.sql"
        private const val TEST_FILE_NAME = "$PROJECT_KEY:test/test_betwnstr.sql"

        @JvmStatic
        @BeforeAll
        fun init() {
            orchestrator.server.provisionProject(PROJECT_KEY, PROJECT_KEY)
            orchestrator.server.associateProjectToQualityProfile(PROJECT_KEY, "plsqlopen", "empty-profile")

            val build = Tests.createSonarScanner()
                .setProjectDir(File("src/integrationTest/resources/projects/metrics/"))
                .setProjectKey(PROJECT_KEY)
                .setProjectName(PROJECT_KEY)
                .setProjectVersion("1.0")
                .setSourceDirs("src")
                .setTestDirs("test")
                .setProperty("sonar.sourceEncoding", "UTF-8")
                .setProperty("sonar.zpa.tests.reportPaths", "test_results.xml")
                .setProperty("sonar.zpa.coverage.reportPaths", "coverage.xml")
                .setDebugLogs(true)
            orchestrator.executeBuild(build)
        }

    }
}
