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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProjectSubprogramDeclarationSemanticIntegrationTest {

    @Test
    fun associatesTopLevelProcedureAndFunctionWithTheExactIndexedFacts() {
        val fileId = FileId("package-body.sql")
        val source = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value NUMBER) IS
              BEGIN
                NULL;
              END work;

              FUNCTION calculate(value NUMBER) RETURN NUMBER IS
              BEGIN
                RETURN value;
              END calculate;
            END p;
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val expected = preparation.index.declarations.filterIsInstance<PackageSubprogramDeclaration>()
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(check.observations).hasSize(2)
        expected.forEach { declaration ->
            val observation = check.observations.single { it.node.declaredName() == declaration.name.originalSpelling }
            assertThat(observation.declaration).isSameAs(declaration)
            assertThat(observation.node.sourceRange(fileId)).isEqualTo(declaration.sourceRange)
            assertThat(observation.declaration?.kind).isEqualTo(
                if (observation.node.type == PlSqlGrammar.PROCEDURE_DECLARATION) {
                    ProjectDeclarationKind.PACKAGE_PROCEDURE
                } else {
                    ProjectDeclarationKind.PACKAGE_FUNCTION
                }
            )
        }
    }

    @Test
    fun mapsOverloadsBySourceIdentityAndExcludesForwardsAndNestedSubprograms() {
        val fileId = FileId("overloads.sql")
        val source = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value NUMBER);

              PROCEDURE work(value NUMBER) IS
              BEGIN
                NULL;
              END work;

              PROCEDURE work(value VARCHAR2) IS
              BEGIN
                NULL;
              END work;

              PROCEDURE outer_work IS
                PROCEDURE nested_work(value NUMBER) IS
                BEGIN
                  NULL;
                END nested_work;
              BEGIN
                NULL;
              END outer_work;

              PROCEDURE private_helper IS
              BEGIN
                NULL;
              END private_helper;
            END p;
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val expected = preparation.index.declarations.filterIsInstance<PackageSubprogramDeclaration>()
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(expected.map { it.name.lookupName }).containsExactly(
            "WORK", "WORK", "OUTER_WORK", "PRIVATE_HELPER"
        )
        assertThat(check.observations.filter { it.declaration != null }.map { it.declaration })
            .containsExactlyInAnyOrderElementsOf(expected)
        expected.forEach { declaration ->
            assertThat(
                check.observations.single { it.declaration?.sourceRange == declaration.sourceRange }.declaration
            ).isSameAs(declaration)
        }
        assertThat(check.observations.filter { it.declaration == null }.map { it.node.declaredName() })
            .containsExactly("work", "nested_work")
        assertThat(check.observations.single { it.node.declaredName() == "nested_work" }.node.parent.parent.type)
            .isEqualTo(PlSqlGrammar.PROCEDURE_DECLARATION)
    }

    @Test
    fun associatesPrivateQuotedBodySubprogramsWithoutNormalizingTheirIdentity() {
        val fileId = FileId("quoted-body.sql")
        val source = """
            CREATE PACKAGE BODY "P" AS
              FUNCTION "Display Name"("Input Value" NUMBER) RETURN NUMBER IS
              BEGIN
                RETURN "Input Value";
              END "Display Name";
            END "P";
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val expected = preparation.index.declarations.filterIsInstance<PackageFunctionDeclaration>().single()
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(check.observations.single().declaration).isSameAs(expected)
        assertThat(expected.owner).isEqualTo(QualifiedName(OracleIdentifier.fromSource("\"P\"")))
        assertThat(expected.name).isEqualTo(OracleIdentifier.fromSource("\"Display Name\""))
        assertThat(expected.parameters.single().name)
            .isEqualTo(OracleIdentifier.fromSource("\"Input Value\""))
    }

    @Test
    fun associatesExternalCallSpecificationsWhenTheParserMaterializesThem() {
        val fileId = FileId("external-body.sql")
        val source = """
            CREATE PACKAGE BODY p AS
              PROCEDURE external_work(value NUMBER) AS
                EXTERNAL NAME "external_work" LIBRARY native_library;
            END p;
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val expected = preparation.index.declarations.filterIsInstance<PackageProcedureDeclaration>().single()
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(check.observations).singleElement().extracting { it.declaration }.isSameAs(expected)
    }

    @Test
    fun packageSpecificationsAndForwardDeclarationsRemainUnassociated() {
        val fileId = FileId("declarations.sql")
        val source = """
            CREATE PACKAGE p AS
              PROCEDURE declared(value NUMBER);
            END p;

            CREATE PACKAGE BODY p AS
              PROCEDURE forward(value NUMBER);
              PROCEDURE implemented(value NUMBER) IS
              BEGIN
                NULL;
              END implemented;
            END p;
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(preparation), check)

        assertThat(check.observations.map { it.node.declaredName() })
            .containsExactly("declared", "forward", "implemented")
        assertThat(check.observations.filter { it.declaration == null }.map { it.node.declaredName() })
            .containsExactly("declared", "forward")
        assertThat(check.observations.single { it.node.declaredName() == "implemented" }.declaration)
            .isNotNull
    }

    @Test
    fun doesNotAssociateWhenTheProjectIsNotPreparedOrHasNoMatchingFact() {
        val fileId = FileId("current.sql")
        val source = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()
        val notPrepared = RecordingCheck()
        val noMatchingFact = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.NOT_PREPARED, notPrepared)
        val otherFile = FileId("other.sql")
        scan(
            fileId,
            source,
            ProjectAnalysisContext.prepared(prepare(otherFile, "CREATE PACKAGE q AS END q;")),
            noMatchingFact
        )

        assertThat(notPrepared.observations).singleElement().extracting { it.declaration }.isNull()
        assertThat(noMatchingFact.observations).singleElement().extracting { it.declaration }.isNull()
    }

    @Test
    fun knownFactsRemainAssociableWhenProjectPreparationAlsoHasFailures() {
        val fileId = FileId("body.sql")
        val source = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()
        val preparation = prepare(fileId, source)
        val incomplete = ProjectIndexPreparationResult(
            preparation.index,
            attemptedFileCount = 2,
            failures = listOf(ProjectIndexPreparationFailure(FileId("failed.sql"), "test"))
        )
        val check = RecordingCheck()

        scan(fileId, source, ProjectAnalysisContext.prepared(incomplete), check)

        assertThat(check.observations.single().declaration)
            .isSameAs(preparation.index.declarations.filterIsInstance<PackageProcedureDeclaration>().single())
    }

    private fun prepare(fileId: FileId, source: String): ProjectIndexPreparationResult =
        ProjectIndexPreparation().prepare(
            listOf(ProjectSource(fileId) { source }),
            concurrent = false
        )

    private fun scan(
        fileId: FileId,
        source: String,
        context: ProjectAnalysisContext,
        check: RecordingCheck
    ) {
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(fileId, source), listOf(check), fileId)
    }

    private class RecordingCheck : PlSqlCheck() {
        val observations = mutableListOf<Observation>()

        init {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION)
        }

        override fun visitNode(node: AstNode) {
            val semanticNode = node as SemanticAstNode
            observations += Observation(semanticNode, semanticNode.projectSubprogramDeclaration)
        }

        data class Observation(
            val node: SemanticAstNode,
            val declaration: PackageSubprogramDeclaration?
        )
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

private fun AstNode.declaredName(): String = tokens[1].originalValue

private fun AstNode.sourceRange(fileId: FileId): SourceRange {
    val first = checkNotNull(tokenOrNull)
    val last = checkNotNull(lastTokenOrNull)
    return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
}
