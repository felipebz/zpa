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
package org.sonar.plsqlopen.utplsql

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.plsqlopen.PlSql
import org.sonar.plsqlopen.symbols.MappedObject
import org.sonar.plsqlopen.symbols.ObjectLocator
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File
import java.nio.charset.StandardCharsets

class UtPlSqlSensorTest {

    private lateinit var sensor: UtPlSqlSensor
    private lateinit var context: SensorContextTester
    private lateinit var objectLocator: ObjectLocator
    private lateinit var analysisWarnings: AnalysisWarnings

    @BeforeEach
    fun setUp() {
        context = SensorContextTester.create(File("src/test/resources/org/sonar/plsqlopen/utplsql/"))
        objectLocator = mock()
        analysisWarnings = spy()
        sensor = UtPlSqlSensor(objectLocator, analysisWarnings)
    }

    @Test
    fun testDescriptor() {
        val descriptor = DefaultSensorDescriptor()
        sensor.describe(descriptor)
        assertThat(descriptor.name()).isEqualTo("ZPA - utPLSQL Report Importer")
        assertThat(descriptor.languages()).containsOnly(PlSql.KEY)
    }

    @Test
    fun shouldImportTestReportWithPaths() {
        val testFile = TestInputFileBuilder("moduleKey", "path/to/file.sql")
            .setType(InputFile.Type.TEST)
            .build()
        context.fileSystem().add(testFile)

        whenever(objectLocator.findTestObject(any(), any())).thenReturn(null)

        context.settings().setProperty(UtPlSqlSensor.TEST_REPORT_PATH_KEY, "test-report-with-paths.xml")
        sensor.execute(context)

        val key = testFile.key()
        assertThat(context.measure(key, CoreMetrics.TESTS).value()).isEqualTo(4)
        assertThat(context.measure(key, CoreMetrics.TEST_FAILURES).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_ERRORS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(5L)
    }

    @Test
    fun shouldImportTestReportWithoutPaths() {
        val testFile = TestInputFileBuilder("moduleKey", "path/to/file.sql")
            .setType(InputFile.Type.TEST)
            .build()
        context.fileSystem().add(testFile)

        whenever(objectLocator.findTestObject(eq("test_package"), any())).thenReturn(
            MappedObject("", PlSqlGrammar.CREATE_PACKAGE_BODY, PlSqlFile.Type.TEST, testFile.path(), testFile)
        )

        context.settings().setProperty(UtPlSqlSensor.TEST_REPORT_PATH_KEY, "test-report-with-paths.xml")
        sensor.execute(context)

        val key = testFile.key()
        assertThat(context.measure(key, CoreMetrics.TESTS).value()).isEqualTo(4)
        assertThat(context.measure(key, CoreMetrics.TEST_FAILURES).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_ERRORS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1)
        assertThat(context.measure(key, CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(5L)
    }

    @Test
    fun invalidTestReport() {
        context.settings().setProperty(UtPlSqlSensor.TEST_REPORT_PATH_KEY, "doesnotexists.xml")
        sensor.execute(context)
        verify(analysisWarnings).addUnique("No utPLSQL test report was found for sonar.zpa.tests.reportPaths using pattern doesnotexists.xml")
    }

    @Test
    fun shouldImportCoverageReportWithPaths() {
        val relativePath = "award_bonus.sql"
        val mainFile = TestInputFileBuilder("moduleKey", relativePath)
            .setType(InputFile.Type.MAIN)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(File(context.fileSystem().baseDir(), relativePath).readText())
            .build()
        context.fileSystem().add(mainFile)

        whenever(objectLocator.findMainObject(any(), any())).thenReturn(null)

        context.settings().setProperty(UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY, "coverage-report-with-paths.xml")
        sensor.execute(context)

        val key = mainFile.key()
        assertThat(context.lineHits(key, 5)).isOne()
        assertThat(context.lineHits(key, 10)).isOne()
        assertThat(context.lineHits(key, 11)).isOne()
        assertThat(context.lineHits(key, 13)).isOne()

        assertThat(context.conditions(key, 10)).isEqualTo(2)
        assertThat(context.coveredConditions(key, 10)).isEqualTo(2)
    }

    @Test
    fun shouldImportCoverageReportWithoutPaths() {
        val relativePath = "betwnstr.sql"
        val mainFile = TestInputFileBuilder("moduleKey", relativePath)
            .setType(InputFile.Type.MAIN)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(File(context.fileSystem().baseDir(), relativePath).readText())
            .build()
        context.fileSystem().add(mainFile)

        whenever(objectLocator.findMainObject(any(), any())).thenReturn(
            MappedObject(
                identifier = "",
                objectType = PlSqlGrammar.CREATE_FUNCTION,
                fileType = PlSqlFile.Type.MAIN,
                path = mainFile.path(),
                inputFile = mainFile,
                firstLine = 3,
                lastLine = 10
            )
        )

        context.settings().setProperty(UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY, "coverage-report-without-paths.xml")
        sensor.execute(context)

        val key = mainFile.key()
        assertThat(context.lineHits(key, 4)).isOne()
        assertThat(context.lineHits(key, 6)).isOne()
        assertThat(context.lineHits(key, 7)).isOne()
        assertThat(context.lineHits(key, 9)).isOne()
    }

    @Test
    fun invalidCoverageReport() {
        context.settings().setProperty(UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY, "doesnotexists.xml")
        sensor.execute(context)
        verify(analysisWarnings).addUnique("No utPLSQL coverage report was found for sonar.zpa.coverage.reportPaths using pattern doesnotexists.xml")
    }

    @Test
    fun shouldNotImportCoverageIfFileDoesNotContainCoveredLined() {
        val relativePath = "betwnstr.sql"
        val mainFile = TestInputFileBuilder("moduleKey", relativePath)
            .setType(InputFile.Type.MAIN)
            .setCharset(StandardCharsets.UTF_8)
            .build()
        context.fileSystem().add(mainFile)

        whenever(objectLocator.findMainObject(any(), any())).thenReturn(
            MappedObject(
                identifier = "",
                objectType = PlSqlGrammar.CREATE_FUNCTION,
                fileType = PlSqlFile.Type.MAIN,
                path = mainFile.path(),
                inputFile = mainFile,
                firstLine = 3,
                lastLine = 10
            )
        )

        context.settings().setProperty(UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY, "coverage-report-all-uncovered.xml")
        sensor.execute(context)

        val key = mainFile.key()
        assertThat(context.lineHits(key, 1)).isNull()
        assertThat(context.lineHits(key, 2)).isNull()
        assertThat(context.lineHits(key, 3)).isNull()
        assertThat(context.lineHits(key, 4)).isNull()
    }
}
