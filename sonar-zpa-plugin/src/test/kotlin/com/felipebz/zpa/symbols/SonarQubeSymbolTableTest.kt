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

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.semantics.HighlightingRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import java.io.File
import java.nio.charset.StandardCharsets

class SonarQubeSymbolTableTest {

    @Test
    fun savesEachDeclarationWithItsReferences() {
        val inputFile = TestInputFileBuilder("key", File("test.sql").path)
                .setContents("v := v + w;\nw")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .build()
        val key = inputFile.key()

        val context = SensorContextTester.create(File(""))
        context.fileSystem().add(inputFile)

        SonarQubeSymbolTable(context, inputFile).save(listOf(
            HighlightingPair(HighlightingRange(1, 0, 1, 1), listOf(HighlightingRange(1, 5, 1, 6))),
            HighlightingPair(HighlightingRange(2, 0, 2, 1), listOf(HighlightingRange(1, 9, 1, 10))),
            HighlightingPair(HighlightingRange(1, 7, 1, 8), emptyList())
        ))

        assertThat(context.referencesForSymbolAt(key, 1, 0))
                .extracting("start.line", "start.lineOffset", "end.lineOffset")
                .containsExactly(tuple(1, 5, 6))
        assertThat(context.referencesForSymbolAt(key, 2, 0))
                .extracting("start.line", "start.lineOffset")
                .containsExactly(tuple(1, 9))
        assertThat(context.referencesForSymbolAt(key, 1, 7)).isEmpty()
    }
}
