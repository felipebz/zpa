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
    fun exposesDeterministicMetadataForCorrelatedPackageFunctions() {
        val specificationFile = FileId("p-spec.sql")
        val bodyFile = FileId("p-body.sql")
        val specification = "CREATE PACKAGE p AS FUNCTION work RETURN NUMBER DETERMINISTIC; END p;"
        val body = "CREATE PACKAGE BODY p AS FUNCTION work RETURN NUMBER IS BEGIN RETURN 1; END work; END p;"
        val probe = FunctionProjectAnalysisProbe()

        scan(
            bodyFile,
            body,
            ProjectAnalysisContext.prepared(prepare(specificationFile to specification, bodyFile to body)),
            probe
        )

        val result = probe.results.single()
        assertThat(result.status).isEqualTo(PackageSpecificationResolution.Status.RESOLVED)
        assertThat(result.getBody().orElseThrow().isDeterministic).isFalse
        assertThat(result.getSpecification().orElseThrow().isDeterministic).isTrue
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

    @Test
    fun distinguishesAmbiguousCallsFromAmbiguousProjectTargets() {
        val packageFile = FileId("test-pkg.sql")
        val bodyFile = FileId("test-pkg-body.sql")
        val helperFile = FileId("helper-pkg.sql")
        val duplicateHelperFile = FileId("duplicate-helper-pkg.sql")
        val duplicateHelperCopyFile = FileId("duplicate-helper-pkg-copy.sql")
        val source = """
            CREATE PACKAGE test_pkg AS
              PROCEDURE setup;
              PROCEDURE required(value NUMBER);
              PROCEDURE defaulted(value NUMBER DEFAULT 1);
              PROCEDURE overloaded;
              PROCEDURE overloaded(value NUMBER);
              PROCEDURE ambiguous_default;
              PROCEDURE ambiguous_default(first NUMBER DEFAULT 1);
              FUNCTION function_target RETURN NUMBER;
            END test_pkg;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY test_pkg AS
              PROCEDURE private_setup IS BEGIN NULL; END;
            END test_pkg;
        """.trimIndent()
        val helper = "CREATE PACKAGE helper_pkg AS PROCEDURE setup; END helper_pkg;"
        val probe = ProcedureResolutionProbe(
            listOf(
                PackageProcedureReference.currentPackage("SETUP"),
                PackageProcedureReference.currentPackage("required"),
                PackageProcedureReference.currentPackage("defaulted"),
                PackageProcedureReference.currentPackage("overloaded"),
                PackageProcedureReference.currentPackage("ambiguous_default"),
                PackageProcedureReference.currentPackage("function_target"),
                PackageProcedureReference.currentPackage("private_setup"),
                PackageProcedureReference.inPackage("HELPER_PKG", "setup"),
                PackageProcedureReference.inPackage("missing_pkg", "setup"),
                PackageProcedureReference.inPackage("duplicate_pkg", "setup")
            )
        )

        scan(
            packageFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    packageFile to source,
                    bodyFile to body,
                    helperFile to helper,
                    duplicateHelperFile to helper.replace("helper_pkg", "duplicate_pkg"),
                    duplicateHelperCopyFile to helper.replace("helper_pkg", "duplicate_pkg")
                )
            ),
            probe
        )

        assertThat(probe.results.map { it.status }).containsExactly(
            PackageProcedureResolution.Status.RESOLVED,
            PackageProcedureResolution.Status.NOT_CALLABLE,
            PackageProcedureResolution.Status.RESOLVED,
            PackageProcedureResolution.Status.RESOLVED,
            PackageProcedureResolution.Status.AMBIGUOUS,
            PackageProcedureResolution.Status.NOT_CALLABLE,
            PackageProcedureResolution.Status.NOT_FOUND,
            PackageProcedureResolution.Status.RESOLVED,
            PackageProcedureResolution.Status.UNKNOWN_TARGET,
            PackageProcedureResolution.Status.AMBIGUOUS_TARGET
        )
    }

    @Test
    fun suppressesProcedureResolutionWhenProjectIsNotPreparedOrIncomplete() {
        val file = FileId("test-pkg.sql")
        val source = "CREATE PACKAGE test_pkg AS PROCEDURE setup; END test_pkg;"
        val notPreparedProbe = ProcedureResolutionProbe(
            listOf(PackageProcedureReference.currentPackage("setup"))
        )
        scan(file, source, ProjectAnalysisContext.NOT_PREPARED, notPreparedProbe)
        assertThat(notPreparedProbe.results.single().status)
            .isEqualTo(PackageProcedureResolution.Status.NOT_PREPARED)

        val prepared = prepare(file to source)
        val incomplete = ProjectIndexPreparationResult(
            prepared.index,
            attemptedFileCount = 2,
            failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )
        val incompleteProbe = ProcedureResolutionProbe(
            listOf(PackageProcedureReference.currentPackage("missing"))
        )
        scan(file, source, ProjectAnalysisContext.prepared(incomplete), incompleteProbe)
        assertThat(incompleteProbe.results.single().status)
            .isEqualTo(PackageProcedureResolution.Status.INCOMPLETE)
    }

    @Test
    fun resolvesOnlyUniquelyIndexedProjectSequences() {
        val sequenceFile = FileId("sequences.sql")
        val targetFile = FileId("target.sql")
        val source = """
            SELECT app.order_seq.NEXTVAL FROM dual;
            SELECT "Seq".NEXTVAL FROM dual;
            SELECT order_seq.NEXTVAL FROM dual;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            targetFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    sequenceFile to "CREATE SEQUENCE app.order_seq; CREATE SEQUENCE \"Seq\";",
                    targetFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.UNKNOWN
        )
    }

    @Test
    fun resolvesCurrvalWithTheSameConservativeSequenceSemantics() {
        val declarationFile = FileId("sequences.sql")
        val referenceFile = FileId("target.sql")
        val source = """
            SELECT order_seq.CURRVAL FROM dual;
            SELECT external_seq.CURRVAL FROM dual;
            SELECT t.CURRVAL FROM some_table t;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE order_seq; CREATE SEQUENCE t;",
                    referenceFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN
        )
    }

    @Test
    fun resolvesUnqualifiedStandaloneSequenceAcrossProjectFiles() {
        val declarationFile = FileId("a.sql")
        val referenceFile = FileId("b.sql")
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            "SELECT order_seq.NEXTVAL FROM dual;",
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE order_seq;",
                    referenceFile to "SELECT order_seq.NEXTVAL FROM dual;"
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
    }

    @Test
    fun keepsUnresolvedAndAmbiguousSequenceReferencesUnknown() {
        val sequenceFile = FileId("sequences.sql")
        val targetFile = FileId("target.sql")
        val source = """
            SELECT t.NEXTVAL FROM some_table t;
            SELECT external_seq.NEXTVAL FROM dual;
            SELECT ambiguous_seq.NEXTVAL FROM dual;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            targetFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    sequenceFile to "CREATE SEQUENCE t; CREATE SEQUENCE ambiguous_seq; CREATE SEQUENCE ambiguous_seq;",
                    targetFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN
        )
    }

    @Test
    fun keepsLocalDmlQualifiersFromResolvingAsSequences() {
        val declarationFile = FileId("sequences.sql")
        val referenceFile = FileId("target.sql")
        val source = """
            UPDATE some_table t
               SET value = CASE WHEN flag = 1 THEN t.NEXTVAL ELSE 0 END;
            MERGE INTO target_table t
            USING source_table src
               ON (t.id = src.id)
            WHEN MATCHED THEN
              UPDATE SET value = CASE
                WHEN flag = 1 THEN t.NEXTVAL
                WHEN flag = 2 THEN src.NEXTVAL
                ELSE order_seq.NEXTVAL
              END;
            INSERT INTO some_table t (value)
              VALUES (CASE WHEN flag = 1 THEN t.NEXTVAL ELSE 0 END);
            INSERT INTO some_table t (value)
              VALUES (CASE WHEN flag = 1 THEN order_seq.NEXTVAL ELSE 0 END);
            UPDATE some_table t
               SET value = CASE WHEN flag = 1 THEN order_seq.NEXTVAL ELSE 0 END;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE t; CREATE SEQUENCE src; CREATE SEQUENCE order_seq;",
                    referenceFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.RESOLVED_SEQUENCE
        )
    }

    @Test
    fun keepsSelectExpressionQualifiersForOrderByAndValuesSources() {
        val declarationFile = FileId("sequences.sql")
        val referenceFile = FileId("target.sql")
        val source = """
            SELECT value
              FROM some_table t
             ORDER BY CASE WHEN flag = 1 THEN t.NEXTVAL ELSE 0 END;
            SELECT value
              FROM some_table t
             ORDER BY CASE WHEN flag = 1 THEN order_seq.NEXTVAL ELSE 0 END;
            SELECT 1
              FROM (VALUES (1)) AS t(value)
             ORDER BY CASE WHEN 1 = 1 THEN t.NEXTVAL ELSE 0 END;
            SELECT 1
              FROM (VALUES (1)) AS t(value)
             ORDER BY CASE WHEN 1 = 1 THEN order_seq.NEXTVAL ELSE 0 END;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE t; CREATE SEQUENCE order_seq;",
                    referenceFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.RESOLVED_SEQUENCE
        )
    }

    @Test
    fun doesNotUseNestedSelectQualifiersForOuterSelectExpression() {
        val declarationFile = FileId("sequences.sql")
        val referenceFile = FileId("target.sql")
        val source = """
            SELECT 1
              FROM some_table s
             ORDER BY CASE
                WHEN flag = 1 THEN t.NEXTVAL
                ELSE (SELECT 1 FROM nested_table t)
              END;
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE t;",
                    referenceFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
    }

    @Test
    fun considersEnclosingSqlQualifiersForCorrelatedReferences() {
        val declarationFile = FileId("sequences.sql")
        val referenceFile = FileId("target.sql")
        val source = """
            SELECT 1
              FROM some_table t
             WHERE EXISTS (
                   SELECT CASE WHEN 1 = 1 THEN t.NEXTVAL ELSE 0 END
                     FROM dual
             );
            SELECT 1
              FROM some_table t
             WHERE EXISTS (
                   SELECT CASE WHEN 1 = 1 THEN order_seq.NEXTVAL ELSE 0 END
                     FROM dual
             );
            UPDATE some_table t
               SET value = (SELECT CASE WHEN 1 = 1 THEN t.NEXTVAL ELSE 0 END FROM dual);
            MERGE INTO target_table t
            USING source_table src
               ON (t.id = src.id)
            WHEN MATCHED THEN
              UPDATE SET value = (SELECT CASE WHEN 1 = 1 THEN src.NEXTVAL ELSE 0 END FROM dual);
        """.trimIndent()
        val probe = SequenceResolutionProbe()

        scan(
            referenceFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    declarationFile to "CREATE SEQUENCE t; CREATE SEQUENCE src; CREATE SEQUENCE order_seq;",
                    referenceFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.RESOLVED_SEQUENCE,
            SequenceReferenceResolution.UNKNOWN,
            SequenceReferenceResolution.UNKNOWN
        )
    }

    @Test
    fun preservesAuthoritativeRecordMemberAsNonSequence() {
        val packageFile = FileId("record-package.sql")
        val targetFile = FileId("record-target.sql")
        val source = "DECLARE value record_pkg.record_type; BEGIN value.nextval := 1; END;"
        val probe = SequenceResolutionProbe()

        scan(
            targetFile,
            source,
            ProjectAnalysisContext.prepared(
                prepare(
                    packageFile to "CREATE PACKAGE record_pkg AS TYPE record_type IS RECORD (nextval NUMBER); END record_pkg;",
                    targetFile to source
                )
            ),
            probe
        )

        assertThat(probe.results).containsExactly(SequenceReferenceResolution.RESOLVED_NON_SEQUENCE)
    }

    @Test
    fun suppressesSequenceResolutionWhenProjectIsNotPreparedOrIncomplete() {
        val file = FileId("target.sql")
        val source = "SELECT order_seq.NEXTVAL FROM dual;"
        val notPreparedProbe = SequenceResolutionProbe()
        scan(file, source, ProjectAnalysisContext.NOT_PREPARED, notPreparedProbe)
        assertThat(notPreparedProbe.results.single()).isEqualTo(SequenceReferenceResolution.UNKNOWN)

        val prepared = prepare(file to source)
        val incomplete = ProjectIndexPreparationResult(
            prepared.index,
            attemptedFileCount = 2,
            failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )
        val incompleteProbe = SequenceResolutionProbe()
        scan(file, source, ProjectAnalysisContext.prepared(incomplete), incompleteProbe)
        assertThat(incompleteProbe.results.single()).isEqualTo(SequenceReferenceResolution.UNKNOWN)
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
        probe: PlSqlCheck
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

    @OptIn(ZpaExperimentalApi::class)
    private class FunctionProjectAnalysisProbe : PlSqlCheck() {
        val results = mutableListOf<PackageSpecificationResolution>()

        init {
            subscribeTo(PlSqlGrammar.FUNCTION_DECLARATION)
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

    @OptIn(ZpaExperimentalApi::class)
    private class ProcedureResolutionProbe(
        private val references: List<PackageProcedureReference>
    ) : PlSqlCheck() {
        val results = mutableListOf<PackageProcedureResolution>()

        init {
            subscribeTo(PlSqlGrammar.CREATE_PACKAGE)
        }

        override fun visitNode(node: AstNode) {
            references.forEach { reference ->
                results += projectAnalysis().resolvePackageProcedure(node, reference)
            }
        }
    }

    @OptIn(ZpaExperimentalApi::class)
    private class SequenceResolutionProbe : PlSqlCheck() {
        val results = mutableListOf<SequenceReferenceResolution>()

        init {
            subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
        }

        override fun visitNode(node: AstNode) {
            if (node.getDescendants(
                    com.felipebz.zpa.api.PlSqlKeyword.NEXTVAL,
                    com.felipebz.zpa.api.PlSqlKeyword.CURRVAL
                ).isNotEmpty()
            ) {
                results += projectAnalysis().resolveSequenceReference(node)
            }
        }
    }
}
