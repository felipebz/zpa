//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
        assertThat(overview.singleStartingWith("Type names").childLabels())
            .contains("p.rec_t → p.rec_t (PACKAGE_TYPE)")
        assertThat(overview.singleStartingWith("Fields of members").childLabels())
            .anyMatch { it.startsWith("n → RECORD_FIELD#0@") && it.endsWith("p.pks:1:42") }
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
        assertThat(overview.singleStartingWith("Type names").childLabels())
            .contains("p.rec_t → p.rec_t (PACKAGE_TYPE)")
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
