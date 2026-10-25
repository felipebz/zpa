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
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Verifies a check against a complete, in-memory project while preserving per-file findings: all [sources] are
 * prepared before any of them is scanned. Expectations are written as for [PlSqlCheckVerifier]; the details a fixture
 * writes (message, columns, cost, secondary locations) are checked.
 */
class ProjectPlSqlCheckVerifier private constructor() {

    companion object {
        /**
         * [conditionalCompilation] applies to the preparation and to the scan alike. With [errorRecovery] off, a source
         * that does not parse fails.
         */
        @JvmStatic
        @JvmOverloads
        fun verify(
            sources: Collection<ProjectTestSource>,
            check: PlSqlCheck,
            metadata: FormsMetadata? = null,
            conditionalCompilation: ConditionalCompilationSettings = ConditionalCompilationSettings.NONE,
            errorRecovery: Boolean = true
        ) {
            require(sources.isNotEmpty()) { "At least one project source is required" }
            require(sources.map { it.fileName }.distinct().size == sources.size) {
                "Project test sources must have unique file names"
            }

            val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, errorRecovery, conditionalCompilation)
            if (!errorRecovery) {
                val parser = PlSqlParser.create(configuration)
                sources.forEach { parser.parse(it.contents) }
            }
            val projectSources = sources.map { source ->
                ProjectSource(FileId(source.fileName)) { source.contents }
            }
            val preparation = ProjectIndexPreparation(ProjectDeclarationExtractor(configuration)).prepare(projectSources, concurrent = false)
            val scanner = AstScanner(
                checks = emptyList(),
                formsMetadata = metadata,
                isErrorRecoveryEnabled = errorRecovery,
                charset = StandardCharsets.UTF_8,
                projectAnalysisContext = ProjectAnalysisContext.prepared(preparation),
                conditionalCompilation = conditionalCompilation
            )

            sources.forEach { source ->
                val file = FixtureFile(source.fileName, source.contents)
                val expectations = PlSqlCheckVerifier()
                scanner.scanFile(file, extraVisitors = listOf(expectations, check), fileId = FileId(source.fileName))
                IssueAssertions.assertIssues(expectations.expectedIssues, check.issues(), source.fileName)
            }
        }
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
