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
package org.sonar.plsqlopen.highlight

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import java.io.File
import java.nio.charset.StandardCharsets

class PlSqlHighlighterVisitorTest {

    @Test
    fun shouldAnalyse_lf(@TempDir dir: File) {
        verifyHighlighting(dir, "\n")
    }

    @Test
    fun shouldAnalyse_crlf(@TempDir dir: File) {
        verifyHighlighting(dir, "\r\n")
    }

    @Test
    fun shouldAnalyse_cr(@TempDir dir: File) {
        verifyHighlighting(dir, "\r")
    }

    private fun verifyHighlighting(baseDir: File, eol: String) {
        val file = File(baseDir, "test.sql")
        val content = File("src/test/resources/highlight/highlight.sql")
            .readText()
            .replace("\r\n", "\n")
            .replace("\n", eol)
        file.writeText(content)

        val inputFile = TestInputFileBuilder("key", "test.sql")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(content)
                .setModuleBaseDir(baseDir.toPath())
                .build()

        val context = SensorContextTester.create(baseDir)
        context.fileSystem().add(inputFile)

        val visitor = PlSqlHighlighterVisitor(context, inputFile)
        TestPlSqlVisitorRunner.scanFile(file, null, visitor)

        val key = inputFile.key()
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
