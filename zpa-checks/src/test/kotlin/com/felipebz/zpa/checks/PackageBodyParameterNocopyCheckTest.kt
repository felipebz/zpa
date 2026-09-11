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
package com.felipebz.zpa.checks

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class PackageBodyParameterNocopyCheckTest : BaseCheckTest() {

    @Test
    fun reportsOnlyTheBodyParameterThatOmitsNocopy() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = """
            CREATE PACKAGE p AS
              PROCEDURE work(
                first_value NUMBER,
                payload IN OUT NOCOPY CLOB,
                last_value NUMBER
              );
            END p;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(
                first_value NUMBER,
                payload IN OUT CLOB,
                last_value NUMBER
              ) IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()

        val check = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)

        assertThat(check.issues()).hasSize(1)
        assertThat(check.issues().single().primaryLocation().startLine()).isEqualTo(4)
        assertThat(check.issues().single().primaryLocation().startLineOffset()).isEqualTo(4)
        assertThat(check.issues().single().primaryLocation().message())
            .isEqualTo("Make this parameter's NOCOPY qualification match its package specification.")
    }

    @Test
    fun reportsNocopyMismatchForFunctionsToo() {
        val specFile = FileId("function-spec.sql")
        val bodyFile = FileId("function-body.sql")
        val specification = "CREATE PACKAGE p AS FUNCTION work(value IN OUT NOCOPY CLOB) RETURN NUMBER; END p;"
        val body = "CREATE PACKAGE BODY p AS FUNCTION work(value IN OUT CLOB) RETURN NUMBER IS BEGIN RETURN 1; END work; END p;"

        val check = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)

        assertThat(check.issues()).hasSize(1)
    }

    @Test
    fun reportsEachMissingNocopyParameterAndAcceptsCompliantPairs() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = """
            CREATE PACKAGE p AS
              PROCEDURE work(
                first_value IN OUT NOCOPY CLOB,
                payload IN OUT NOCOPY CLOB,
                last_value IN OUT NOCOPY CLOB
              );
            END p;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(
                first_value IN OUT CLOB,
                payload IN OUT CLOB,
                last_value IN OUT NOCOPY CLOB
              ) IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()

        val mismatching = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)
        assertThat(mismatching.issues().map { it.primaryLocation().startLine() }).containsExactly(3, 4)

        val bothNocopy = body.replace("first_value IN OUT CLOB", "first_value IN OUT NOCOPY CLOB")
            .replace("payload IN OUT CLOB", "payload IN OUT NOCOPY CLOB")
        assertThat(run(specFile to specification, bodyFile to bothNocopy, bodyFile = bodyFile).issues()).isEmpty()

        val bothWithoutNocopy = specification.replace(" IN OUT NOCOPY", " IN OUT")
        assertThat(run(specFile to bothWithoutNocopy, bodyFile to body.replace(" IN OUT NOCOPY", " IN OUT"), bodyFile = bodyFile).issues())
            .isEmpty()

        val specificationWithoutNocopy = specification.replace(" IN OUT NOCOPY", " IN OUT")
        val bodyWithOnlyOneNocopy = body.replace(" IN OUT NOCOPY", " IN OUT")
            .replace("first_value IN OUT CLOB", "first_value IN OUT NOCOPY CLOB")
        assertThat(run(specFile to specificationWithoutNocopy, bodyFile to bodyWithOnlyOneNocopy, bodyFile = bodyFile).issues())
            .hasSize(1)
    }

    @Test
    fun ignoresPrivateHelpersForWhichNoSpecificationExists() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE implemented(value IN OUT NOCOPY CLOB); END p;"
        val body = """
            CREATE PACKAGE BODY p AS
              PROCEDURE forward(value IN OUT NOCOPY CLOB);

              PROCEDURE implemented(value IN OUT CLOB) IS
              BEGIN
                NULL;
              END implemented;

              PROCEDURE outer_work IS
                PROCEDURE nested_work(value IN OUT CLOB) IS
                BEGIN
                  NULL;
                END nested_work;
              BEGIN
                NULL;
              END outer_work;
            END p;
        """.trimIndent()

        val check = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)

        assertThat(check.issues()).hasSize(1)
        assertThat(check.issues().single().primaryLocation().startLine()).isEqualTo(4)
    }

    @Test
    fun correlatesOverloadsIndependently() {
        val specFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN OUT NOCOPY NUMBER);
              PROCEDURE work(value IN OUT NOCOPY CLOB);
            END p;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value IN OUT NUMBER) IS
              BEGIN
                NULL;
              END work;

              PROCEDURE work(value IN OUT NOCOPY CLOB) IS
              BEGIN
                NULL;
              END work;
            END p;
        """.trimIndent()

        val check = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)

        assertThat(check.issues()).hasSize(1)
        assertThat(check.issues().single().primaryLocation().startLine()).isEqualTo(2)
    }

    @Test
    fun suppressesAmbiguousAndIncompleteProjectResults() {
        val specFile = FileId("a-spec.sql")
        val duplicateSpecFile = FileId("b-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS PROCEDURE work(value IN OUT NOCOPY CLOB); END p;"
        val body = "CREATE PACKAGE BODY p AS PROCEDURE work(value IN OUT CLOB) IS BEGIN NULL; END work; END p;"

        val ambiguous = run(
            specFile to specification,
            duplicateSpecFile to specification,
            bodyFile to body,
            bodyFile = bodyFile
        )
        assertThat(ambiguous.issues()).isEmpty()

        val complete = prepare(specFile to specification, bodyFile to body)
        val incomplete = ProjectIndexPreparationResult(
            complete.index,
            attemptedFileCount = 3,
            failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )
        val incompleteCheck = run(
            context = ProjectAnalysisContext.prepared(incomplete),
            bodyFile = bodyFile,
            body = body
        )
        assertThat(incompleteCheck.issues()).isEmpty()
    }

    @Test
    fun notPreparedAnalysisProducesNoFinding() {
        val bodyFile = FileId("p-body.sql")
        val check = run(
            context = ProjectAnalysisContext.NOT_PREPARED,
            bodyFile = bodyFile,
            body = "CREATE PACKAGE BODY p AS PROCEDURE work(value IN OUT CLOB) IS BEGIN NULL; END work; END p;"
        )

        assertThat(check.issues()).isEmpty()
    }

    @Test
    fun preservesQuotedIdentifierCorrelation() {
        val specFile = FileId("quoted-spec.sql")
        val bodyFile = FileId("quoted-body.sql")
        val specification = "CREATE PACKAGE \"P\" AS PROCEDURE \"Work\"(\"Payload\" IN OUT NOCOPY CLOB); END \"P\";"
        val body = "CREATE PACKAGE BODY \"P\" AS PROCEDURE \"Work\"(\"Payload\" IN OUT CLOB) IS BEGIN NULL; END \"Work\"; END \"P\";"

        val check = run(specFile to specification, bodyFile to body, bodyFile = bodyFile)

        assertThat(check.issues()).hasSize(1)
    }

    private fun run(
        vararg sources: Pair<FileId, String>,
        bodyFile: FileId,
        context: ProjectAnalysisContext = prepare(*sources).let(ProjectAnalysisContext::prepared),
        body: String = sources.single { it.first == bodyFile }.second
    ): PackageBodyParameterNocopyCheck {
        val check = PackageBodyParameterNocopyCheck()
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(bodyFile, body), listOf(check), bodyFile)
        return check
    }

    private fun prepare(vararg sources: Pair<FileId, String>) =
        ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        )

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
