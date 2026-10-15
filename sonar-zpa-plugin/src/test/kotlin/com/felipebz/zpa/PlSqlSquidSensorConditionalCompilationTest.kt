/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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

import com.felipebz.zpa.symbols.ObjectLocator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.utils.MessageException
import java.io.File
import java.nio.charset.StandardCharsets

class PlSqlSquidSensorConditionalCompilationTest {

    /** Parses only when `$$legacy` is evaluated (to NULL), which needs some configuration. */
    private val source = "CREATE PROCEDURE p IS BEGIN \$IF \$\$legacy \$THEN \$ELSE NULL; \$END END;\n"

    private fun analyze(vararg properties: Pair<String, String>): Pair<SensorContextTester, InputFile> {
        val settings = MapSettings()
        properties.forEach { (key, value) -> settings.setProperty(key, value) }
        val factory = mock(FileLinesContextFactory::class.java)
        `when`(factory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        val sensor = PlSqlSquidSensor(ActiveRulesBuilder().build(), settings.asConfig(), mock(NoSonarFilter::class.java), factory, null, ObjectLocator())
        val context = SensorContextTester.create(File("."))
        val inputFile = TestInputFileBuilder("key", "cc.sql")
            .setLanguage(PlSql.KEY).setCharset(StandardCharsets.UTF_8).setContents(source).build()
        context.fileSystem().add(inputFile)
        sensor.execute(context)
        return context to inputFile
    }

    @Test
    fun withoutConfigurationConditionsAreNotEvaluated() {
        val (context, file) = analyze()
        assertThat(context.measure(file.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(0)
    }

    @Test
    fun flagsAndVersionReachTheScanner() {
        for (property in listOf(PlSqlPlugin.CCFLAGS_KEY to "debug:true", PlSqlPlugin.DATABASE_VERSION_KEY to "19")) {
            val (context, file) = analyze(property)
            assertThat(context.measure(file.key(), CoreMetrics.FUNCTIONS).value()).describedAs(property.first).isEqualTo(1)
        }
    }

    @Test
    fun invalidValuesFailWithThePropertyName() {
        assertThatThrownBy { analyze(PlSqlPlugin.CCFLAGS_KEY to "debug:maybe") }
            .isInstanceOf(MessageException::class.java).hasMessageContaining(PlSqlPlugin.CCFLAGS_KEY)
        assertThatThrownBy { analyze(PlSqlPlugin.DATABASE_VERSION_KEY to "19c") }
            .isInstanceOf(MessageException::class.java).hasMessageContaining(PlSqlPlugin.DATABASE_VERSION_KEY)
    }
}
