package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.datatype.RecordDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProjectTypeSemanticIntegrationTest {
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun qualifiedCrossFileTypeIsDecoratedWithoutReplacingLegacyDatatype() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val packageDeclarations = extractor.extract(
            packageFile,
            "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;"
        )
        val expected = packageDeclarations.filterIsInstance<PackageTypeDeclaration>().single()
        val recordingCheck = RecordingCheck()

        scan(
            listOf(
                packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
                useFile to "DECLARE value p.t; BEGIN NULL; END;"
            ),
            useFile,
            recordingCheck
        )

        val datatype = recordingCheck.datatypes.single()
        val resolution = datatype.projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat((resolution as ProjectTypeResolution.Resolved).declaration).isEqualTo(expected)
        assertThat(datatype.plSqlDatatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun projectResolutionIsIndependentOfFileAnalysisOrder() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN NULL; END;"
        )

        val packageFirst = scan(sources, useFile, RecordingCheck()).second
        val useFirst = scan(sources.asReversed(), useFile, RecordingCheck()).second

        assertThat(packageFirst).isEqualTo(useFirst)
        assertThat(packageFirst).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
    }

    @Test
    fun packageOwnerAllowsSafeResolutionOfUnqualifiedTypeInPackageBody() {
        val packageFile = FileId("package.sql")
        val bodyFile = FileId("package_body.sql")
        val recordingCheck = RecordingCheck()

        scan(
            listOf(
                packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
                bodyFile to "CREATE PACKAGE BODY p AS PROCEDURE q(value IN t) IS BEGIN NULL; END q; END p;"
            ),
            bodyFile,
            recordingCheck
        )

        val resolution = recordingCheck.datatypes.single().projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat((resolution as ProjectTypeResolution.Resolved).declaration)
            .isInstanceOf(PackageTypeDeclaration::class.java)
        assertThat((resolution.declaration as PackageTypeDeclaration).qualifiedName)
            .isEqualTo(QualifiedName(listOf(
                OracleIdentifier.fromSource("P"),
                OracleIdentifier.fromSource("T")
            )))
    }

    @Test
    fun packageSubtypeIsAttachedWithoutResolvingItsBaseType() {
        val declarationFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val declaration = extractor.extract(
            declarationFile,
            "CREATE PACKAGE p AS SUBTYPE small IS NUMBER; END p;"
        ).filterIsInstance<PackageSubtypeDeclaration>().single()
        val recordingCheck = RecordingCheck()

        scan(
            listOf(
                declarationFile to "CREATE PACKAGE p AS SUBTYPE small IS NUMBER; END p;",
                useFile to "DECLARE value p.small; BEGIN NULL; END;"
            ),
            useFile,
            recordingCheck
        )

        val resolution = recordingCheck.datatypes.single().projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        val resolved = resolution as ProjectTypeResolution.Resolved
        assertThat(resolved.declaration).isEqualTo(declaration)
        assertThat(resolved.declaration).isInstanceOf(PackageSubtypeDeclaration::class.java)
    }

    @Test
    fun lexicalTypeResolutionTakesPrecedenceOverProjectResolution() {
        val projectTypeFile = FileId("project.sql")
        val useFile = FileId("use.sql")
        val recordingCheck = RecordingCheck()

        val result = scan(
            listOf(
                projectTypeFile to "CREATE TYPE t AS OBJECT (id NUMBER);",
                useFile to "DECLARE TYPE t IS RECORD (id NUMBER); value t; BEGIN NULL; END;"
            ),
            useFile,
            recordingCheck
        )

        val datatype = recordingCheck.datatypes.single { it.tokenValue.equals("t", ignoreCase = true) }
        assertThat(datatype.projectTypeResolution).isNull()
        assertThat(result.first.symbols.single { it.name.equals("value", ignoreCase = true) }.datatype)
            .isInstanceOf(RecordDatatype::class.java)
    }

    @Test
    fun duplicateProjectTypesRemainAmbiguous() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val useFile = FileId("use.sql")
        val recordingCheck = RecordingCheck()

        scan(
            listOf(
                firstFile to "CREATE TYPE t AS OBJECT (id NUMBER);",
                secondFile to "CREATE TYPE t AS OBJECT (id NUMBER);",
                useFile to "DECLARE value t; BEGIN NULL; END;"
            ),
            useFile,
            recordingCheck
        )

        val resolution = recordingCheck.datatypes.single().projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
        assertThat((resolution as ProjectTypeResolution.Ambiguous).candidates.map { it.fileId.value })
            .containsExactly("first.sql", "second.sql")
    }

    @Test
    fun incompleteProjectIndexDoesNotAttachKnownCandidateAsUnique() {
        val knownFile = FileId("known.sql")
        val brokenFile = FileId("broken.sql")
        val useFile = FileId("use.sql")
        val sourceExtractor = ProjectDeclarationExtractor()
        val preparation = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { fileId, source ->
            if (fileId == brokenFile) error("broken declaration pass")
            sourceExtractor.extract(fileId, source)
        })
        val context = ProjectAnalysisContext.prepared(
            preparation.prepare(
                listOf(
                    ProjectSource(knownFile) { "CREATE TYPE t AS OBJECT (id NUMBER);" },
                    ProjectSource(brokenFile) { "not relevant" },
                    ProjectSource(useFile) { "DECLARE value t; BEGIN NULL; END;" }
                ),
                concurrent = false
            )
        )
        val recordingCheck = RecordingCheck()
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(useFile, "DECLARE value t; BEGIN NULL; END;"), listOf(recordingCheck), useFile)

        val resolution = recordingCheck.datatypes.single().projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat((resolution as ProjectTypeResolution.IncompleteIndex).knownCandidates).hasSize(1)
    }

    @Test
    fun missingProjectTypeKeepsUnknownDatatypeWithoutIssue() {
        val useFile = FileId("use.sql")
        val recordingCheck = RecordingCheck()
        val result = scan(
            listOf(useFile to "DECLARE value external_type; BEGIN NULL; END;"),
            useFile,
            recordingCheck
        ).first

        val datatype = recordingCheck.datatypes.single()
        assertThat(datatype.projectTypeResolution).isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThat(datatype.plSqlDatatype).isSameAs(UnknownDatatype)
        assertThat(result.issues).isEmpty()
    }

    private fun scan(
        sources: List<Pair<FileId, String>>,
        observedFile: FileId,
        recordingCheck: RecordingCheck
    ): Pair<com.felipebz.zpa.squid.AstScannerResult, ProjectTypeResolution?> {
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(
                sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
                concurrent = false
            )
        )
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        var result: com.felipebz.zpa.squid.AstScannerResult? = null
        sources.forEach { (fileId, source) ->
            val visitors = if (fileId == observedFile) listOf(recordingCheck) else emptyList()
            result = scanner.scanFile(FixtureFile(fileId, source), visitors, fileId)
        }
        return result!! to recordingCheck.datatypes.singleOrNull()?.projectTypeResolution
    }

    private class RecordingCheck : PlSqlCheck() {
        val datatypes = mutableListOf<SemanticAstNode>()

        init {
            subscribeTo(PlSqlGrammar.DATATYPE)
        }

        override fun visitNode(node: AstNode) {
            datatypes += node as SemanticAstNode
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
