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
package com.felipebz.zpa.internal

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@OptIn(ZpaInternalApi::class)
class BuiltInProjectAnalysisQueriesTest {

    @Test
    fun injectsTheCapabilityBeforeCallbacksAndResolvesTheSpecification() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN OUT NOCOPY CLOB DEFAULT NULL);
            END p;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value IN OUT CLOB) IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()
        val preparation = prepare(
            specFile to specification,
            bodyFile to body
        )
        val check = QueryProbe()

        scan(bodyFile, body, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(check.callbackSawInjectedCapability).isTrue
        assertThat(check.results).singleElement().isInstanceOf(BuiltInPackageSpecificationResolution.Resolved::class.java)
        val result = check.results.single() as BuiltInPackageSpecificationResolution.Resolved
        assertThat(result.body.kind).isEqualTo(BuiltInPackageSubprogramKind.PROCEDURE)
        assertThat(result.specification.kind).isEqualTo(BuiltInPackageSubprogramKind.PROCEDURE)
        assertThat(result.body.parameters).extracting<Int> { it.ordinal }.containsExactly(1)
        assertThat(result.specification.parameters).extracting<Int> { it.ordinal }.containsExactly(1)
        assertThat(result.body.parameters.single().nocopy).isFalse
        assertThat(result.specification.parameters.single().nocopy).isTrue
        assertThat(result.body.parameters.single().defaultPresent).isFalse
        assertThat(result.specification.parameters.single().defaultPresent).isTrue
    }

    @Test
    fun mapsFunctionDeclarationsToFunctionViews() {
        val specFile = FileId("function-spec.sql")
        val bodyFile = FileId("function-body.sql")
        val specification = "CREATE PACKAGE p AS FUNCTION value RETURN NUMBER; END p;"
        val body = "CREATE PACKAGE BODY p AS FUNCTION value RETURN NUMBER IS BEGIN RETURN 1; END value; END p;"
        val check = QueryProbe()

        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(specFile to specification, bodyFile to body)),
            check
        )

        val result = check.results.single() as BuiltInPackageSpecificationResolution.Resolved
        assertThat(result.body.kind).isEqualTo(BuiltInPackageSubprogramKind.FUNCTION)
        assertThat(result.specification.kind).isEqualTo(BuiltInPackageSubprogramKind.FUNCTION)
    }

    @Test
    fun distinguishesNonApplicableNodesFromPreparedNotFound() {
        val specFile = FileId("spec.sql")
        val bodyFile = FileId("body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work; END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE helper IS BEGIN NULL; END helper; END p;"

        val specCheck = QueryProbe()
        scan(
            specFile,
            specification,
            ProjectAnalysisContext.prepared(prepare(specFile to specification)),
            specCheck
        )
        assertThat(specCheck.results).containsExactly(BuiltInPackageSpecificationResolution.NotApplicable)

        val bodyCheck = QueryProbe()
        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(bodyFile to body)),
            bodyCheck
        )
        assertThat(bodyCheck.results).containsExactly(BuiltInPackageSpecificationResolution.NotFound)
    }

    @Test
    fun preservesAmbiguousAndIncompleteStatesWithoutExposingProjectFacts() {
        val specOne = FileId("a-spec.sql")
        val specTwo = FileId("b-spec.sql")
        val bodyFile = FileId("body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work(value NUMBER) IS BEGIN NULL; END work; END p;"

        val ambiguousCheck = QueryProbe()
        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(
                specOne to specification,
                specTwo to specification,
                bodyFile to body
            )),
            ambiguousCheck
        )
        assertThat(ambiguousCheck.results).containsExactly(
            BuiltInPackageSpecificationResolution.Ambiguous(candidateCount = 2)
        )

        val prepared = prepare(specOne to specification, bodyFile to body)
        val incomplete = ProjectIndexPreparationResult(
            prepared.index,
            attemptedFileCount = 3,
            failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )
        val incompleteCheck = QueryProbe()
        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(incomplete),
            incompleteCheck
        )
        assertThat(incompleteCheck.results).containsExactly(
            BuiltInPackageSpecificationResolution.Incomplete(
                knownCandidateCount = 1,
                failureCount = 1
            )
        )
    }

    @Test
    fun reusesOneCapabilityWithinAnAnalysisAndReinjectsASeparateCurrentCapability() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work; END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;"
        val check = QueryProbe()
        val preparedContext = ProjectAnalysisContext.prepared(prepare(specFile to specification, bodyFile to body))
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, preparedContext)

        scanner.scanFile(FixtureFile(bodyFile, body), listOf(check), bodyFile)
        scanner.scanFile(FixtureFile(specFile, specification), listOf(check), specFile)

        assertThat(check.capabilities).hasSize(2)
        assertThat(check.capabilities[0]).isSameAs(check.capabilities[1])
        assertThat(check.results[0]).isInstanceOf(BuiltInPackageSpecificationResolution.Resolved::class.java)
        assertThat(check.results[1]).isEqualTo(BuiltInPackageSpecificationResolution.NotApplicable)

        val notPreparedCheck = QueryProbe()
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, ProjectAnalysisContext.NOT_PREPARED)
            .scanFile(FixtureFile(bodyFile, body), listOf(notPreparedCheck), bodyFile)

        assertThat(notPreparedCheck.capabilities).hasSize(1)
        assertThat(notPreparedCheck.results).containsExactly(BuiltInPackageSpecificationResolution.NotPrepared)
        assertThat(notPreparedCheck.capabilities.single()).isNotSameAs(check.capabilities[0])
    }

    @Test
    fun preparedEmptyContextIsNotReportedAsNotPrepared() {
        val fileId = FileId("body.sql")
        val check = QueryProbe()
        val empty = ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(ProjectSymbolIndexBuilder().build(), 0, emptyList())
        )

        scan(
            fileId,
            "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;",
            empty,
            check
        )

        assertThat(check.results).containsExactly(BuiltInPackageSpecificationResolution.NotApplicable)
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
        check: QueryProbe
    ) {
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(fileId, source), listOf(check), fileId)
    }

    private class QueryProbe : PlSqlCheck(), BuiltInProjectAnalysisConsumer {
        val capabilities = mutableListOf<BuiltInProjectAnalysisQueries>()
        val results = mutableListOf<BuiltInPackageSpecificationResolution>()
        var callbackSawInjectedCapability = true
            private set
        private var queries: BuiltInProjectAnalysisQueries? = null

        init {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION)
        }

        override fun setProjectAnalysisQueries(queries: BuiltInProjectAnalysisQueries) {
            this.queries = queries
            capabilities += queries
        }

        override fun visitNode(node: AstNode) {
            val semanticNode = node as SemanticAstNode
            val currentQueries = queries
            if (currentQueries == null) {
                callbackSawInjectedCapability = false
            } else {
                results += currentQueries.resolvePackageSpecification(semanticNode)
            }
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
