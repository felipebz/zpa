//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utplsql

import com.felipebz.zpa.PlSql
import com.felipebz.zpa.symbols.ObjectLocator
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.notifications.AnalysisWarnings

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
