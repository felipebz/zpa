/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.ConfigurationBridge
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.plsqlopen.symbols.MappedObject
import org.sonar.plsqlopen.symbols.ObjectLocator
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File


class UtPlSqlTestSensorTest {

    private lateinit var settings: MapSettings
    private lateinit var sensor: UtPlSqlTestSensor
    private lateinit var context: SensorContextTester
    private lateinit var objectLocator: ObjectLocator
    private lateinit var analysisWarnings: AnalysisWarnings

    @BeforeEach
    fun setUp() {
        context = SensorContextTester.create(File("src/test/resources/org/sonar/plsqlopen/utplsql/"))
        settings = MapSettings()
        objectLocator = mock()
        analysisWarnings = spy()
        sensor = UtPlSqlTestSensor(ConfigurationBridge(settings), objectLocator, analysisWarnings)
    }

    @Test
    fun testDescriptor() {
        val descriptor = DefaultSensorDescriptor()
        sensor.describe(descriptor)
        assertThat(descriptor.name()).isEqualTo("Z PL/SQL Analyzer - utPLSQL Test Report Importer")
        assertThat(descriptor.languages()).containsOnly(PlSql.KEY)
    }

    @Test
    fun shouldImportReportWithPaths() {
        val testFile = TestInputFileBuilder("moduleKey", "path/to/file.sql")
            .setType(InputFile.Type.TEST)
            .build()
        context.fileSystem().add(testFile)

        whenever(objectLocator.findTestObject(any(), any())).thenReturn(null)

        settings.setProperty(UtPlSqlTestSensor.REPORT_PATH_KEY, "test-report-with-paths.xml")
        sensor.execute(context)

        val key = testFile.key()
        assertThat(context.measure(key, CoreMetrics.TESTS).value()).isEqualTo(4)
        assertThat(context.measure(key, CoreMetrics.TEST_FAILURES).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_ERRORS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(5L)
    }

    @Test
    fun shouldImportReportWithoutPaths() {
        val testFile = TestInputFileBuilder("moduleKey", "path/to/file.sql")
            .setType(InputFile.Type.TEST)
            .build()
        context.fileSystem().add(testFile)

        whenever(objectLocator.findTestObject(eq("test_package"), any())).thenReturn(
            MappedObject("", PlSqlGrammar.CREATE_PACKAGE_BODY, PlSqlFile.Type.TEST, testFile.path(), testFile))

        settings.setProperty(UtPlSqlTestSensor.REPORT_PATH_KEY, "test-report-with-paths.xml")
        sensor.execute(context)

        val key = testFile.key()
        assertThat(context.measure(key, CoreMetrics.TESTS).value()).isEqualTo(4)
        assertThat(context.measure(key, CoreMetrics.TEST_FAILURES).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_ERRORS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(5L)
    }

    @Test
    fun invalidReport() {
        settings.setProperty(UtPlSqlTestSensor.REPORT_PATH_KEY, "doesnotexists.xml")
        sensor.execute(context)
        verify(analysisWarnings).addUnique("No utPLSQL test report was found for sonar.zpa.utplsql.reportPaths using pattern doesnotexists.xml")
    }
}
