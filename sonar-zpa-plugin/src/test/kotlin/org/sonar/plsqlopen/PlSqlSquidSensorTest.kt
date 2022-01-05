/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
import org.mockito.Mockito.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder
import org.sonar.api.batch.rule.internal.NewActiveRule
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.rule.RuleKey
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class PlSqlSquidSensorTest {

    private lateinit var sensor: PlSqlSquidSensor
    private lateinit var context: SensorContextTester
    private lateinit var fileLinesContext: FileLinesContext

    @BeforeEach
    fun setUp() {
        val activeRules = ActiveRulesBuilder()
                .addRule(NewActiveRule.Builder()
                    .setRuleKey(RuleKey.of(PlSqlRuleRepository.KEY, "EmptyBlock"))
                    .setName("Print Statement Usage").build())
                .build()
        context = SensorContextTester.create(File("."))

        val fileLinesContextFactory = mock(FileLinesContextFactory::class.java)
        fileLinesContext = mock(FileLinesContext::class.java)
        `when`(fileLinesContextFactory.createFor(any(InputFile::class.java))).thenReturn(fileLinesContext)

        sensor = PlSqlSquidSensor(activeRules, MapSettings().asConfig(), mock(NoSonarFilter::class.java), fileLinesContextFactory, null)
    }

    @Test
    fun testDescriptor() {
        val descriptor = DefaultSensorDescriptor()
        sensor.describe(descriptor)
        assertThat(descriptor.name()).isEqualTo("Z PL/SQL Analyzer")
        assertThat(descriptor.languages()).containsOnly(PlSql.KEY)
    }

    @Test
    fun shouldAnalyse() {
        val relativePath = "src/test/resources/org/sonar/plsqlopen/code.sql"
        val inputFile = TestInputFileBuilder("key", relativePath)
                .setLanguage(PlSql.KEY)
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(File(relativePath).readText())
                .setModuleBaseDir(Paths.get(""))
                .build()

        context.fileSystem().add(inputFile)

        sensor.execute(context)

        val key = inputFile.key()

        //assertThat(context.measure(key, CoreMetrics.FILES).value()).isEqualTo(1);
        assertThat(context.measure(key, CoreMetrics.NCLOC).value()).isEqualTo(18)
        assertThat(context.measure(key, CoreMetrics.COMMENT_LINES).value()).isEqualTo(2)
        assertThat(context.measure(key, CoreMetrics.COMPLEXITY).value()).isEqualTo(6)
        assertThat(context.measure(key, CoreMetrics.FUNCTIONS).value()).isEqualTo(2)
        assertThat(context.measure(key, CoreMetrics.STATEMENTS).value()).isEqualTo(7)
        verify(fileLinesContext, times(7))
            .setIntValue(eq(CoreMetrics.EXECUTABLE_LINES_DATA_KEY), anyInt(), eq(1))
        verify(fileLinesContext).save()
    }

    @Test
    fun shouldAnalyseTestFile() {
        val relativePath = "src/test/resources/org/sonar/plsqlopen/test.sql"
        val inputFile = TestInputFileBuilder("key", relativePath)
                .setLanguage(PlSql.KEY)
                .setType(InputFile.Type.TEST)
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(File(relativePath).readText())
                .setModuleBaseDir(Paths.get(""))
                .build()

        context.fileSystem().add(inputFile)

        sensor.execute(context)

        val key = inputFile.key()

        // shouldn't save metrics for test files
        assertThat(context.measure(key, CoreMetrics.NCLOC)).isNull()
        assertThat(context.measure(key, CoreMetrics.COMMENT_LINES)).isNull()
        assertThat(context.measure(key, CoreMetrics.COMPLEXITY)).isNull()
        assertThat(context.measure(key, CoreMetrics.FUNCTIONS)).isNull()
        assertThat(context.measure(key, CoreMetrics.STATEMENTS)).isNull()
        verifyNoInteractions(fileLinesContext)

        // but should save highlighting data
        assertThat(context.highlightingTypeAt(key, 1, lineOffset(1))).containsExactly(TypeOfText.KEYWORD)
        assertThat(context.highlightingTypeAt(key, 2, lineOffset(3))).containsExactly(TypeOfText.COMMENT)
        assertThat(context.highlightingTypeAt(key, 3, lineOffset(3))).containsExactly(TypeOfText.STRUCTURED_COMMENT)
        assertThat(context.highlightingTypeAt(key, 6, lineOffset(8))).containsExactly(TypeOfText.STRING)
        assertThat(context.highlightingTypeAt(key, 7, lineOffset(1))).containsExactly(TypeOfText.KEYWORD)
    }

    private fun lineOffset(offset: Int): Int {
        return offset - 1
    }

}
