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
package com.felipebz.zpa.utplsql

import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.notifications.AnalysisWarnings
import com.felipebz.zpa.PlSql
import com.felipebz.zpa.symbols.ObjectLocator

class UtPlSqlSensor(objectLocator: ObjectLocator, analysisWarnings: AnalysisWarnings) : Sensor {

    private val testResultImporter = TestResultImporter(objectLocator, analysisWarnings)
    private val coverageResultImporter = CoverageResultImporter(objectLocator, analysisWarnings)

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.name("ZPA - utPLSQL Report Importer").onlyOnLanguage(PlSql.KEY)
    }

    override fun execute(context: SensorContext) {
        testResultImporter.execute(context)
        coverageResultImporter.execute(context)
    }

    companion object {
        const val TEST_REPORT_PATH_KEY = "sonar.zpa.tests.reportPaths"
        const val DEFAULT_TEST_REPORT_PATH = "utplsql-test.xml"
        const val COVERAGE_REPORT_PATH_KEY = "sonar.zpa.coverage.reportPaths"
        const val DEFAULT_COVERAGE_REPORT_PATH = "utplsql-coverage.xml"
    }

}
