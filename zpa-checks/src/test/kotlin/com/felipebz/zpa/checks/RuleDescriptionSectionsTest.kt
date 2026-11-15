//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/** The `<!-- section: fix -->` line separates a description's explanation from its examples of how to fix it. */
class RuleDescriptionSectionsTest {

    private val folders = listOf("plsqlopen", "plsqlopen_pt_BR")
        .map { File("src/main/resources/org/sonar/l10n/$it/rules/plsql") }

    private fun markers(file: File) = file.readLines().count { it.trim() == MARKER }

    @Test
    fun atMostOneMarkerPerFile() {
        val files = folders.flatMap { it.listFiles { f -> f.extension == "html" }!!.toList() }
        assertThat(files).isNotEmpty()
        assertThat(files.filter { markers(it) > 1 }.map { it.path }).isEmpty()
    }

    @Test
    fun bothLanguagesAgreeOnTheMarker() {
        val (english, portuguese) = folders
        val disagreeing = english.listFiles { f -> f.extension == "html" }!!
            .filter { (markers(it) > 0) != (markers(File(portuguese, it.name)) > 0) }
            .map { it.name }
        assertThat(disagreeing).isEmpty()
    }

    @Test
    fun theMarkerPrecedesTheFirstDiffExample() {
        val misplaced = folders.flatMap { it.listFiles { f -> f.extension == "html" }!!.toList() }
            .filter { it.readText().contains("data-diff-type") }
            .filter {
                val lines = it.readLines()
                val marker = lines.indexOfFirst { line -> line.trim() == MARKER }
                marker < 0 || marker > lines.indexOfFirst { line -> line.contains("<pre data-diff-id") }
            }
            .map { it.path }
        assertThat(misplaced).isEmpty()
    }

    private companion object {
        const val MARKER = "<!-- section: fix -->"
    }
}
