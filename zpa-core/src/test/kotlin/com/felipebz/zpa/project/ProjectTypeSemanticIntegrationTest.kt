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

        val result = scan(
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
        val symbol = result.first.symbols.single { it.name.equals("value", ignoreCase = true) }
        assertThat(symbol.projectTypeDeclaration).isSameAs(resolution.declaration)
        assertThat(symbol.datatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun projectResolutionIsIndependentOfFileAnalysisOrder() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN NULL; END;"
        )

        val first = scan(sources, useFile, RecordingCheck())
        val reversed = scan(sources.asReversed(), useFile, RecordingCheck())
        val packageFirst = first.second
        val useFirst = reversed.second

        assertThat(packageFirst).isEqualTo(useFirst)
        assertThat(packageFirst).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        val target = (packageFirst as ProjectTypeResolution.Resolved).declaration
        assertThat(first.first.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isSameAs(target)
        assertThat(reversed.first.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isEqualTo(target)
    }

    @Test
    fun packageOwnerAllowsSafeResolutionOfUnqualifiedTypeInPackageBody() {
        val packageFile = FileId("package.sql")
        val bodyFile = FileId("package_body.sql")
        val recordingCheck = RecordingCheck()

        val result = scan(
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
        assertThat(result.first.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isSameAs(resolution.declaration)
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

        val result = scan(
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
        val symbol = result.first.symbols.single { it.name.equals("value", true) }
        assertThat(symbol.projectTypeDeclaration).isSameAs(resolved.declaration)
        assertThat(symbol.datatype).isSameAs(UnknownDatatype)
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
        assertThat(result.first.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration).isNull()
    }

    @Test
    fun duplicateProjectTypesRemainAmbiguous() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val useFile = FileId("use.sql")
        val recordingCheck = RecordingCheck()

        val result = scan(
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
        assertThat(result.first.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration).isNull()
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
        val result = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(useFile, "DECLARE value t; BEGIN NULL; END;"), listOf(recordingCheck), useFile)

        val resolution = recordingCheck.datatypes.single().projectTypeResolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat((resolution as ProjectTypeResolution.IncompleteIndex).knownCandidates).hasSize(1)
        assertThat(result.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration).isNull()
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
        val symbol = result.symbols.single { it.name.equals("value", true) }
        assertThat(symbol.projectTypeDeclaration).isNull()
        assertThat(symbol.datatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun directDeclaredTypesPropagateToExistingSymbols() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val packageSource = "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;"
        val expected = extractor.extract(packageFile, packageSource).filterIsInstance<PackageTypeDeclaration>().single()
        val declarations = listOf(
            "CREATE PROCEDURE q(value p.t) IS BEGIN NULL; END;" to "value",
            "CREATE FUNCTION f RETURN p.t IS BEGIN RETURN NULL; END;" to "f",
            "CREATE PACKAGE q AS FUNCTION f RETURN p.t; END q;" to "f",
            "DECLARE CURSOR c(value p.t) IS SELECT 1 FROM dual; BEGIN NULL; END;" to "value",
            "DECLARE SUBTYPE local_type IS p.t; BEGIN NULL; END;" to "local_type",
            "CREATE PACKAGE q AS value p.t; END q;" to "value"
        )
        declarations.forEach { (source, symbolName) ->
            val check = RecordingCheck()
            val result = scan(listOf(packageFile to packageSource, useFile to source), useFile, check).first
            val resolution = check.datatypes.single().projectTypeResolution as ProjectTypeResolution.Resolved
            assertThat(resolution.declaration).isEqualTo(expected)
            val symbol = result.symbols.single { it.name.equals(symbolName, true) }
            assertThat(symbol.projectTypeDeclaration).describedAs(source).isSameAs(resolution.declaration)
            assertThat(symbol.datatype).describedAs(source).isSameAs(UnknownDatatype)
        }
    }

    @Test
    fun explicitlyTypedIterandReceivesProjectSubtypeTarget() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            packageFile to "CREATE PACKAGE p AS SUBTYPE small IS NUMBER; END p;",
            useFile to "BEGIN FOR i p.small IN 1..2 LOOP NULL; END LOOP; END;"
        ), useFile, check).first
        val resolution = check.datatypes.single().projectTypeResolution as ProjectTypeResolution.Resolved
        assertThat(resolution.declaration).isInstanceOf(PackageSubtypeDeclaration::class.java)
        val symbol = result.symbols.single { it.name.equals("i", true) }
        assertThat(symbol.projectTypeDeclaration).isSameAs(resolution.declaration)
        assertThat(symbol.datatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun nestedComponentTypesDoNotBecomeTheEnclosingSymbolsTarget() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to """
                DECLARE
                  TYPE r IS RECORD (field p.t);
                  TYPE c IS TABLE OF p.t;
                  TYPE v IS VARRAY(10) OF p.t;
                BEGIN NULL; END;
            """.trimIndent()
        ), useFile, check).first
        assertThat(check.datatypes).hasSize(3)
        check.datatypes.forEach {
            assertThat(it.projectTypeResolution).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        }
        assertThat(result.symbols).hasSize(3)
        result.symbols.forEach { assertThat(it.projectTypeDeclaration).isNull() }
    }

    @Test
    fun builtinsAndAnchorsDoNotGainProjectTargets() {
        val file = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(file to """
            DECLARE value NUMBER; anchored value%TYPE; row_value external_table%ROWTYPE;
            BEGIN NULL; END;
        """.trimIndent()), file, check).first
        assertThat(check.datatypes).hasSize(3)
        check.datatypes.forEach { assertThat(it.projectTypeResolution).isNull() }
        result.symbols.forEach { assertThat(it.projectTypeDeclaration).isNull() }
        assertThat(result.symbols.single { it.name.equals("value", true) }.datatype)
            .isNotSameAs(UnknownDatatype)
    }

    @Test
    fun unpreparedAnalysisDoesNotPropagateProjectTargets() {
        val file = FileId("use.sql")
        val check = RecordingCheck()
        val result = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8)
            .scanFile(FixtureFile(file, "DECLARE value p.t; BEGIN NULL; END;"), listOf(check), file)
        assertThat(check.datatypes.single().projectTypeResolution).isNull()
        assertThat(result.symbols.single().projectTypeDeclaration).isNull()
        assertThat(result.symbols.single().datatype).isSameAs(UnknownDatatype)
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
            val scanned = scanner.scanFile(FixtureFile(fileId, source), visitors, fileId)
            if (fileId == observedFile) result = scanned
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
