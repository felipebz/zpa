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
package com.felipebz.zpa.checks.verifier

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Verifies a check against a complete, in-memory project while preserving per-file findings.
 *
 * The existing [PlSqlCheckVerifier] intentionally remains a single-file, legacy-semantics
 * verifier. This verifier is additive and prepares all [sources] before scanning any of them.
 */
class ProjectPlSqlCheckVerifier private constructor() {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun verify(
            sources: Collection<ProjectTestSource>,
            check: PlSqlCheck,
            metadata: FormsMetadata? = null
        ) {
            require(sources.isNotEmpty()) { "At least one project source is required" }
            require(sources.map { it.fileName }.distinct().size == sources.size) {
                "Project test sources must have unique file names"
            }

            val projectSources = sources.map { source ->
                ProjectSource(FileId(source.fileName)) { source.contents }
            }
            val preparation = ProjectIndexPreparation().prepare(projectSources, concurrent = false)
            val projectContext = ProjectAnalysisContext.prepared(preparation)
            val scanner = AstScanner(
                checks = emptyList(),
                formsMetadata = metadata,
                isErrorRecoveryEnabled = true,
                charset = StandardCharsets.UTF_8,
                projectAnalysisContext = projectContext
            )

            sources.forEach { source ->
                val file = FixtureFile(source.fileName, source.contents)
                scanner.scanFile(file, extraVisitors = listOf(check), fileId = FileId(source.fileName))
                verifyIssues(source, check.issues())
            }
        }

        private fun verifyIssues(source: ProjectTestSource, actualIssues: List<PlSqlCheck.PreciseIssue>) {
            val expectedIssues = expectedIssues(source.contents)
            val actual = actualIssues.sortedBy { it.primaryLocation().startLine() }
            if (actual.size != expectedIssues.size) {
                throw AssertionError(
                    "Unexpected issue count in ${source.fileName}: expected ${expectedIssues.size}, " +
                        "but was ${actual.size}"
                )
            }

            expectedIssues.zip(actual).forEach { (expected, issue) ->
                val actualLine = issue.primaryLocation().startLine()
                if (actualLine != expected.line) {
                    throw AssertionError(
                        "Unexpected issue location in ${source.fileName}: expected line ${expected.line}, " +
                            "but was $actualLine"
                    )
                }
                expected.message?.let { expectedMessage ->
                    val actualMessage = issue.primaryLocation().message()
                    if (actualMessage != expectedMessage) {
                        throw AssertionError(
                            "Unexpected issue message in ${source.fileName} at line ${expected.line}: " +
                                "expected '$expectedMessage', but was '$actualMessage'"
                        )
                    }
                }
            }
        }

        private fun expectedIssues(contents: String): List<ExpectedIssue> =
            contents.lineSequence().mapIndexedNotNull { index, line ->
                val markerIndex = line.indexOf("-- Noncompliant")
                if (markerIndex < 0) {
                    null
                } else {
                    val marker = line.substring(markerIndex + "-- Noncompliant".length).trim()
                    val message = if (marker.startsWith("{{") && marker.endsWith("}}")) {
                        marker.substring(2, marker.length - 2)
                    } else {
                        null
                    }
                    ExpectedIssue(index + 1, message)
                }
            }.toList()

        private data class ExpectedIssue(val line: Int, val message: String?)
    }

    private class FixtureFile(
        private val name: String,
        private val source: String
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
