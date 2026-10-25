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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.ObjectLocator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

/** The symbol highlighting the sensor saves is the scanner's highlighting pairs. */
class PlSqlSquidSensorHighlightingTest {

    @ParameterizedTest
    @EnumSource(InputFile.Type::class)
    fun savesTheScannerPairs(type: InputFile.Type) {
        val relativePath = "src/test/resources/com/felipebz/zpa/symbols/highlighting.sql"
        val text = File(relativePath).readText()
        val inputFile = TestInputFileBuilder("key", relativePath)
            .setLanguage(PlSql.KEY)
            .setType(type)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(text)
            .setModuleBaseDir(Paths.get(""))
            .build()
        val context = SensorContextTester.create(File("."))
        context.fileSystem().add(inputFile)
        val fileLinesContextFactory = mock(FileLinesContextFactory::class.java)
        `when`(fileLinesContextFactory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        PlSqlSquidSensor(ActiveRulesBuilder().build(), MapSettings().asConfig(), mock(NoSonarFilter::class.java), fileLinesContextFactory, null, ObjectLocator())
            .execute(context)
        val key = inputFile.key()

        // Each segment of b.v, the cursor variable of OPEN … FOR and CLOSE; the member n after a(1) is no reference of n.
        assertThat(context.referencesForSymbolAt(key, 1, 2)).extracting("start.line", "start.lineOffset").containsExactly(tuple(10, 2))
        assertThat(context.referencesForSymbolAt(key, 3, 2)).extracting("start.line", "start.lineOffset").containsExactly(tuple(10, 4))
        assertThat(context.referencesForSymbolAt(key, 4, 2)).extracting("start.line", "start.lineOffset").containsExactly(tuple(11, 7), tuple(12, 8))
        assertThat(context.referencesForSymbolAt(key, 8, 2)).extracting("start.line", "start.lineOffset").containsExactly(tuple(13, 2))

        val file = object : PlSqlFile {
            override fun contents() = text
            override fun fileName() = "highlighting.sql"
            override fun path(): Path = Path.of(relativePath)
            override fun type() = PlSqlFile.Type.MAIN
        }
        val pairs = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8).scanFile(file).highlightingPairs
        assertThat(pairs).isNotEmpty()
        for (pair in pairs) {
            val saved = context.referencesForSymbolAt(key, pair.declaration.startLine, pair.declaration.startColumn)!!
                .map { listOf(it.start().line(), it.start().lineOffset(), it.end().line(), it.end().lineOffset()) }
            assertThat(saved).containsExactlyInAnyOrderElementsOf(pair.references.map { listOf(it.startLine, it.startColumn, it.endLine, it.endColumn) })
        }
    }
}
