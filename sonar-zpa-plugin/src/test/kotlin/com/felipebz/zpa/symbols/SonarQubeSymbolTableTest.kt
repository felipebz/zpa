//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
