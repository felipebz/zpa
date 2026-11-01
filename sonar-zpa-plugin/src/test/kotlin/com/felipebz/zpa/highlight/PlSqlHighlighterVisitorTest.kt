//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.highlight

import com.felipebz.zpa.TestPlSqlVisitorRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.internal.SensorContextTester
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
