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
package com.felipebz.zpa.api.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@OptIn(ZpaExperimentalApi::class)
class ProjectAnalysisTest {

    @Test
    fun resolvesACorrelatedPackageSpecificationWithImmutablePublicViews() {
        val specificationFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work(first NUMBER, value IN OUT NOCOPY CLOB); END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work(first NUMBER, value IN OUT CLOB) IS BEGIN NULL; END work; END p;"
        val probe = ProjectAnalysisProbe()

        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(specificationFile to specification, bodyFile to body)),
            probe
        )

        val result = probe.results.single()
        assertThat(result.status).isEqualTo(PackageSpecificationResolution.Status.RESOLVED)
        assertThat(result.getBody()).isPresent
        assertThat(result.getSpecification()).isPresent
        assertThat(result.getBody().orElseThrow().kind).isEqualTo(PackageSubprogramKind.PROCEDURE)
        assertThat(result.getBody().orElseThrow().parameters).extracting<Int> { it.ordinal }.containsExactly(1, 2)
        assertThat(result.getBody().orElseThrow().parameters[1].isNocopy).isFalse
        assertThat(result.getSpecification().orElseThrow().parameters[1].isNocopy).isTrue
    }

    @Test
    fun replacesPreparedStateOnDirectAndSubsequentPreparedScans() {
        val specificationFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work; END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;"
        val probe = ProjectAnalysisProbe()

        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(specificationFile to specification, bodyFile to body)),
            probe
        )

        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(body)
        probe.scanFile(PlSqlVisitorContext(root, FixtureFile(bodyFile, body), null))

        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(bodyFile to body)),
            probe
        )

        assertThat(probe.results.map { it.status }).containsExactly(
            PackageSpecificationResolution.Status.RESOLVED,
            PackageSpecificationResolution.Status.NOT_PREPARED,
            PackageSpecificationResolution.Status.NOT_FOUND
        )
    }

    @Test
    fun preservesAmbiguousIncompleteAndNotApplicableStates() {
        val specificationFile = FileId("p-spec.sql")
        val duplicateSpecificationFile = FileId("p-spec-copy.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work; END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;"

        val ambiguousProbe = ProjectAnalysisProbe()
        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(
                prepare(
                    specificationFile to specification,
                    duplicateSpecificationFile to specification,
                    bodyFile to body
                )
            ),
            ambiguousProbe
        )
        assertThat(ambiguousProbe.results.single().status)
            .isEqualTo(PackageSpecificationResolution.Status.AMBIGUOUS)

        val prepared = prepare(specificationFile to specification, bodyFile to body)
        val incomplete = ProjectIndexPreparationResult(
            prepared.index,
            attemptedFileCount = 3,
            failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )
        val incompleteProbe = ProjectAnalysisProbe()
        scan(bodyFile, body, ProjectAnalysisContext.prepared(incomplete), incompleteProbe)
        assertThat(incompleteProbe.results.single().status)
            .isEqualTo(PackageSpecificationResolution.Status.INCOMPLETE)

        val nonApplicableProbe = ProjectAnalysisProbe()
        scan(
            specificationFile,
            specification,
            ProjectAnalysisContext.prepared(
                ProjectIndexPreparationResult(ProjectSymbolIndexBuilder().build(), 0, emptyList())
            ),
            nonApplicableProbe
        )
        assertThat(nonApplicableProbe.results.single().status)
            .isEqualTo(PackageSpecificationResolution.Status.NOT_APPLICABLE)
    }

    private fun prepare(vararg sources: Pair<FileId, String>) =
        ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        )

    private fun scan(
        fileId: FileId,
        source: String,
        context: ProjectAnalysisContext,
        probe: ProjectAnalysisProbe
    ) {
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(fileId, source), listOf(probe), fileId)
    }

    @OptIn(ZpaExperimentalApi::class)
    private class ProjectAnalysisProbe : PlSqlCheck() {
        val results = mutableListOf<PackageSpecificationResolution>()

        init {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION)
        }

        override fun visitNode(node: AstNode) {
            results += projectAnalysis().resolvePackageSpecification(node)
        }
    }

    private class FixtureFile(
        private val fileId: FileId,
        private val source: String
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = fileId.value
        override fun path(): Path = Path.of(fileId.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
