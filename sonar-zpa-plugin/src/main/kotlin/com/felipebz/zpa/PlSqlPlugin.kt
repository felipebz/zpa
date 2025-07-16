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
package com.felipebz.zpa

import org.sonar.api.Plugin
import org.sonar.api.PropertyType
import org.sonar.api.config.PropertyDefinition
import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.log.SonarQubeLoggers
import com.felipebz.zpa.utils.log.Loggers
import com.felipebz.zpa.utplsql.UtPlSqlSensor

class PlSqlPlugin : Plugin {

    override fun define(context: Plugin.Context) {
        Loggers.factory = SonarQubeLoggers()

        context.addExtensions(
            PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                .deprecatedKey(OLD_FILE_SUFFIXES_KEY)
                .name("File Suffixes")
                .description("Comma-separated list of suffixes of PL/SQL files to analyze.")
                .category(DEFAULT_CATEGORY)
                .subCategory(GENERAL)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .defaultValue("sql,pkg,pks,pkb")
                .multiValues(true)
                .build(),
            PropertyDefinition.builder(FORMS_METADATA_KEY)
                .name("Oracle Forms metadata file")
                .description("Path to the JSON file with the Oracle Forms metadata.")
                .category(DEFAULT_CATEGORY)
                .subCategory(GENERAL)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .build(),
            PropertyDefinition.builder(ERROR_RECOVERY_KEY)
                .name("Parse error recovery")
                .description("Defines mode for error handling of parsing errors. 'False' (strict) breaks after an error or 'True' (tolerant, default) continues.")
                .category(DEFAULT_CATEGORY)
                .subCategory(GENERAL)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .type(PropertyType.BOOLEAN)
                .defaultValue("true")
                .build(),
            PropertyDefinition.builder(CONCURRENT_EXECUTION_KEY)
                .name("Enable concurrent execution")
                .description("Enable concurrent analysis of files.")
                .category(DEFAULT_CATEGORY)
                .subCategory(GENERAL)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .type(PropertyType.BOOLEAN)
                .defaultValue("true")
                .build(),

            PlSql::class.java,
            PlSqlProfile::class.java,
            PlSqlSquidSensor::class.java,
            PlSqlRuleRepository::class.java,
            ObjectLocator::class.java,
        )

        addUtPlSqlExtensions(context)
    }

    private fun addUtPlSqlExtensions(context: Plugin.Context) {
        context.addExtensions(
            PropertyDefinition.builder(UtPlSqlSensor.TEST_REPORT_PATH_KEY)
                .name("Path to the utPLSQL test report(s)")
                .description("Paths (absolute or relative) to report files with utPLSQL test execution data.")
                .category(DEFAULT_CATEGORY)
                .subCategory(TEST_AND_COVERAGE)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .defaultValue(UtPlSqlSensor.DEFAULT_TEST_REPORT_PATH)
                .multiValues(true)
                .build(),
            PropertyDefinition.builder(UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY)
                .name("Path to the utPLSQL coverage report(s)")
                .description("Paths (absolute or relative) to report files with utPLSQL coverage data.")
                .category(DEFAULT_CATEGORY)
                .subCategory(TEST_AND_COVERAGE)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .defaultValue(UtPlSqlSensor.DEFAULT_COVERAGE_REPORT_PATH)
                .multiValues(true)
                .build(),

            UtPlSqlSensor::class.java,
        )
    }

    companion object {
        private const val DEFAULT_CATEGORY = "ZPA"
        private const val GENERAL = "General"
        private const val TEST_AND_COVERAGE = "Tests and Coverage"
        internal const val FILE_SUFFIXES_KEY = "sonar.plsqlopen.file.suffixes"
        internal const val OLD_FILE_SUFFIXES_KEY = "sonar.zpa.file.suffixes"
        internal const val FORMS_METADATA_KEY = "sonar.zpa.forms.metadata"
        internal const val ERROR_RECOVERY_KEY = "sonar.zpa.errorRecoveryEnabled"
        internal const val CONCURRENT_EXECUTION_KEY = "sonar.zpa.concurrentExecution"
    }

}
