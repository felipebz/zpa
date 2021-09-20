/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.metrics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class CpdVisitorTest {
    private val context = SensorContextTester.create(File(BASE_DIR))

    @Test
    fun scanFile() {
        val inputFile = inputFile()
        context.fileSystem().add(inputFile)

        val visitor = CpdVisitor(context, inputFile)
        TestPlSqlVisitorRunner.scanFile(File(Paths.get(BASE_DIR, FILE).toString()), null, visitor)
        val cpdTokenLines = context.cpdTokens(inputFile.key())
        assertThat(cpdTokenLines).hasSize(17)
    }

    private fun inputFile(): DefaultInputFile {
        val inputFile = TestInputFileBuilder("key", "cpd.sql")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(File(BASE_DIR, FILE).readText())
                .setModuleBaseDir(Paths.get(BASE_DIR))
                .build()

        context.fileSystem().add(inputFile)

        return inputFile
    }

    companion object {
        private const val BASE_DIR = "src/test/resources/metrics"
        private const val FILE = "cpd.sql"
    }

}
