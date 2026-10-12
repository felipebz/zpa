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
package com.felipebz.flr.internal.toolkit

import com.felipebz.zpa.tooling.ProjectOverviewNode
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class ProjectSourcesOverviewTest {
    @TempDir
    lateinit var dir: Path

    private fun model(): SourceCodeModel {
        val config = ZpaConfigurationModel()
        config.projectSourcesProperty.value = dir.toString()
        return SourceCodeModel(config)
    }

    private fun write(name: String, text: String): Path =
        Files.writeString(dir.resolve(name), text).toAbsolutePath().normalize()

    @Test
    fun recordTypeFromAnotherFileIsResolved() {
        val spec = write("p.pks", RECORD_SPEC)

        val overview = model().apply { setSourceCode(CALLER) }.projectOverview!!

        assertThat(overview.labels()).anyMatch { it.startsWith("Preparation: PREPARED_WITH_DECLARATIONS · 2/2 files") }
        assertThat(overview.single("Project files (1)").childLabels()).containsExactly("$spec · 2 declarations")
        assertThat(overview.labels()).noneMatch { it.startsWith("Packages (") }
        assertThat(overview.singleStartingWith("Type references").childLabels())
            .contains("p.rec_t → Resolved p.rec_t (PACKAGE_TYPE)")
        assertThat(overview.singleStartingWith("RECORD members").childLabels())
            .anyMatch { it.endsWith("→ field n #0 of p.rec_t") }
    }

    @Test
    fun openedFileInsideTheProjectFolderIsIndexedOnce() {
        write("p.pks", RECORD_SPEC)
        val caller = write("caller.sql", CALLER)
        val model = model()

        model.setSourceCode(caller.toFile(), UTF_8)
        assertIndexedOnce(model.projectOverview!!)

        model.setSourceCode(CALLER)
        assertIndexedOnce(model.projectOverview!!)
    }

    private fun assertIndexedOnce(overview: ProjectOverviewNode) {
        assertThat(overview.labels()).anyMatch { it.startsWith("Preparation: PREPARED_WITH_DECLARATIONS · 2/2 files") }
        assertThat(overview.singleStartingWith("Type references").childLabels())
            .contains("p.rec_t → Resolved p.rec_t (PACKAGE_TYPE)")
    }

    @Test
    fun specificationInAnotherFileIsLabelledWithItsFileAndNotHighlighted() {
        val spec = write("p.pks", "CREATE PACKAGE p AS PROCEDURE run; END p;")

        val overview = model()
            .apply { setSourceCode("CREATE PACKAGE BODY p AS PROCEDURE run IS BEGIN NULL; END; END p;") }
            .projectOverview!!

        val body = overview.single("PACKAGE BODY p · 1 subprogram(s)")
        val specification = body.all().single { it.label == "specification: RESOLVED" }.children.single()
        assertThat(specification.label).startsWith("p.run @$spec:")
        assertThat(specification.sourceRange).isNull()
    }

    @Test
    fun unreadableFileIsReportedAsPreparationFailure() {
        val bad = dir.resolve("bad.sql").toAbsolutePath().normalize()
        Files.write(bad, byteArrayOf(0xC3.toByte(), 0x28))

        val overview = model().apply { setSourceCode(CALLER) }.projectOverview!!

        assertThat(overview.singleStartingWith("Preparation:").childLabels())
            .contains("failure: $bad: java.nio.charset.MalformedInputException")
        assertThat(overview.single("Project files (1)").childLabels())
            .containsExactly("$bad · failed: java.nio.charset.MalformedInputException")
    }

    private fun ProjectOverviewNode.all(): Sequence<ProjectOverviewNode> = sequence {
        yield(this@all)
        for (child in children) yieldAll(child.all())
    }

    private fun ProjectOverviewNode.labels(): List<String> = all().map { it.label }.toList()

    private fun ProjectOverviewNode.childLabels(): List<String> = children.map { it.label }

    private fun ProjectOverviewNode.single(label: String): ProjectOverviewNode = all().single { it.label == label }

    private fun ProjectOverviewNode.singleStartingWith(prefix: String): ProjectOverviewNode =
        all().single { it.label.startsWith(prefix) }

    private companion object {
        const val RECORD_SPEC = "CREATE PACKAGE p AS TYPE rec_t IS RECORD (n NUMBER); END p;"
        const val CALLER = "DECLARE x p.rec_t; BEGIN x.n := 1; END;"
    }
}
