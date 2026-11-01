//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ProjectSourceFilesTest {
    @TempDir
    lateinit var base: Path

    private val sep = File.pathSeparator

    private fun touch(path: Path): Path {
        Files.createDirectories(path.parent)
        Files.writeString(path, "")
        return path.toAbsolutePath().normalize()
    }

    @Test
    fun foldersAreFilteredBySuffixWhileExplicitFilesAreAlwaysIncluded() {
        val dir = base.resolve("proj")
        val a = touch(dir.resolve("a.pks"))
        val b = touch(dir.resolve("b.PKB"))
        touch(dir.resolve("c.txt"))
        val d = touch(dir.resolve("sub/d.sql"))
        val e = touch(base.resolve("other/e.txt"))

        val collected = ProjectSourceFiles.collect("$dir$sep$e$sep$dir/sub$sep${base.resolve("missing")}", "sql,pkg,pks,pkb")

        assertThat(collected).containsExactlyElementsOf(listOf(a, b, d, e).sorted())
    }

    @Test
    fun validationReportsTheFirstMissingEntry() {
        assertThat(ProjectSourceFiles.validate("$base$sep/no/such")).isEqualTo("Path not found: /no/such")
        assertThat(ProjectSourceFiles.validate("")).isEmpty()
    }

    @Test
    fun suffixesAreNormalizedAndRequired() {
        assertThat(ProjectSourceFiles.suffixes(".SQL, pks")).isEqualTo(setOf("sql", "pks"))
        assertThat(ProjectSourceFiles.validateSuffixes(" , ")).isEqualTo("At least one suffix is required")
    }

    @Test
    fun mergeAppendsAbsolutePicksAndDropsExactDuplicates() {
        val y = File("y").absoluteFile.toPath().normalize()
        val x = File("x").absoluteFile.toPath().normalize()

        assertThat(ProjectSourceFiles.merge("x", listOf(File("y"), File("x"), File("y"))))
            .isEqualTo("x$sep$y$sep$x")
    }
}
