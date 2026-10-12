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
