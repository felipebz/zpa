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

class ProjectRecordMemberSemanticIntegrationTest {
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun resolvesCrossFileRecordMemberWithoutChangingLegacyDatatype() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val packageSource = """
            CREATE PACKAGE p AS
              TYPE t IS RECORD (id NUMBER, customer VARCHAR2(100));
            END p;
        """.trimIndent()
        val memberCheck = RecordingCheck()
        val result = scan(
            listOf(
                packageFile to packageSource,
                useFile to """
                    DECLARE
                      value p.t;
                      result VARCHAR2(100);
                    BEGIN
                      value.customer := 'x';
                      result := value.customer;
                    END;
                """.trimIndent()
            ), useFile, memberCheck
        )
        val expectedType = extractor.extract(packageFile, packageSource)
            .filterIsInstance<PackageTypeDeclaration>().single()
        val expectedField = expectedType.recordFields.single { it.name.lookupName == "CUSTOMER" }
        val value = result.symbols.single { it.name.equals("value", true) }

        assertThat(value.projectTypeDeclaration).isEqualTo(expectedType)
        assertThat(value.datatype).isSameAs(UnknownDatatype)
        val resolutions = memberCheck.members
            .filter { it.memberParts() == listOf("value", "customer") }
            .map { it.projectRecordMemberResolution }
        assertThat(resolutions).hasSize(2)
        assertThat(resolutions).allSatisfy {
            assertThat(it).isEqualTo(ProjectRecordMemberResolution.Resolved(expectedType, expectedField))
        }
    }

    @Test
    fun fileOrderProducesTheSameMemberTarget() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; END;"
        )
        val forwardCheck = RecordingCheck()
        val reverseCheck = RecordingCheck()
        val forward = scan(sources, useFile, forwardCheck)
        val reverse = scan(sources.asReversed(), useFile, reverseCheck)

        val forwardResolution = forwardCheck.members.single { it.memberParts() == listOf("value", "id") }
            .projectRecordMemberResolution
        val reverseResolution = reverseCheck.members.single { it.memberParts() == listOf("value", "id") }
            .projectRecordMemberResolution
        assertThat(forwardResolution).isEqualTo(reverseResolution)
        assertThat(forward.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isEqualTo(reverse.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
    }

    @Test
    fun packageLocalTypeResolvesItsMember() {
        val packageFile = FileId("package.sql")
        val bodyFile = FileId("body.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            bodyFile to "CREATE PACKAGE BODY p AS PROCEDURE q IS value t; BEGIN value.id := 1; END q; END p;"
        ), bodyFile, check)
        val resolution = check.members.single { it.memberParts() == listOf("value", "id") }
            .projectRecordMemberResolution

        assertThat(resolution).isInstanceOf(ProjectRecordMemberResolution.Resolved::class.java)
        assertThat((resolution as ProjectRecordMemberResolution.Resolved).field.name.lookupName).isEqualTo("ID")
        assertThat(result.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isInstanceOf(PackageTypeDeclaration::class.java)
    }

    @Test
    fun quotedFieldUsesQuotedIdentifierSemantics() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val source = "CREATE PACKAGE p AS TYPE t IS RECORD (\"Mixed Field\" NUMBER); END p;"
        val check = RecordingCheck()
        scan(listOf(
            packageFile to source,
            useFile to "DECLARE value p.t; BEGIN value.\"Mixed Field\" := 1; END;"
        ), useFile, check)

        val resolution = check.members.single { it.memberParts() == listOf("value", "\"Mixed Field\"") }
            .projectRecordMemberResolution
        assertThat(resolution).isInstanceOf(ProjectRecordMemberResolution.Resolved::class.java)
        assertThat((resolution as ProjectRecordMemberResolution.Resolved).field.name)
            .isEqualTo(OracleIdentifier.fromSource("\"Mixed Field\""))
    }

    @Test
    fun missingAmbiguousIncompleteAndNonRecordBasesRemainConservative() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val source = "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); TYPE c IS TABLE OF NUMBER; END p;"
        val duplicateFile = FileId("duplicate.sql")
        val incompleteFile = FileId("broken.sql")
        val preparation = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { fileId, text ->
            if (fileId == incompleteFile) error("temporary preparation failure")
            extractor.extract(fileId, text)
        })
        val context = ProjectAnalysisContext.prepared(preparation.prepare(listOf(
            ProjectSource(packageFile) { source },
            ProjectSource(duplicateFile) { "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;" },
            ProjectSource(incompleteFile) { "ignored" },
            ProjectSource(useFile) { "DECLARE value p.t; BEGIN value.missing := 1; END;" }
        ), concurrent = false))
        val check = RecordingCheck()
        val result = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(FixtureFile(useFile, "DECLARE value p.t; BEGIN value.missing := 1; END;"), listOf(check), useFile)

        assertThat(check.members.single { it.memberParts() == listOf("value", "missing") }
            .projectRecordMemberResolution).isNull()
        assertThat(result.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration).isNull()
    }

    @Test
    fun missingProjectFieldIsExplicitlyNotFoundWithoutAnIssue() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN value.missing := 1; END;"
        ), useFile, check)
        val resolution = check.members.single { it.memberParts() == listOf("value", "missing") }
            .projectRecordMemberResolution

        assertThat(resolution).isEqualTo(ProjectRecordMemberResolution.NotFound(
            extractor.extract(packageFile, "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;")
                .filterIsInstance<PackageTypeDeclaration>().single()
        ))
        assertThat(result.issues).isEmpty()
    }

    @Test
    fun ambiguousProjectBaseDoesNotResolveARecordMember() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            firstFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            secondFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; END;"
        ), useFile, check)

        assertThat(check.members.single { it.memberParts() == listOf("value", "id") }
            .projectRecordMemberResolution).isNull()
        assertThat(result.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration).isNull()
    }

    @Test
    fun nonRecordProjectBaseRetainsUnsupportedMemberResolution() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS TABLE OF NUMBER; END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; END;"
        ), useFile, check)

        val resolution = check.members.single { it.memberParts() == listOf("value", "id") }
            .projectRecordMemberResolution
        assertThat(resolution).isInstanceOf(ProjectRecordMemberResolution.UnsupportedType::class.java)
    }

    @Test
    fun localRecordAndQualifiedNonRecordExpressionsAreNotProjectMembers() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val result = scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); TYPE c IS TABLE OF NUMBER; END p;",
            useFile to """
                DECLARE
                  TYPE t IS RECORD (id NUMBER);
                  value t;
                BEGIN
                  value.id := 1;
                  p.some_procedure;
                END;
            """.trimIndent()
        ), useFile, check)

        assertThat(check.members.filter { it.memberParts() == listOf("value", "id") }
            .single().projectRecordMemberResolution).isNull()
        assertThat(check.members.filter { it.memberParts() == listOf("p", "some_procedure") }
            .single().projectRecordMemberResolution).isNull()
        assertThat(result.symbols.single { it.name.equals("value", true) }.datatype)
            .isInstanceOf(RecordDatatype::class.java)
    }

    @Test
    fun multiHopMemberExpressionsRemainUnresolved() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            packageFile to "CREATE PACKAGE p AS TYPE t IS RECORD (child child_pkg.child_type); END p;",
            useFile to "DECLARE value p.t; BEGIN value.child.name := 1; END;"
        ), useFile, check)

        assertThat(check.members.filter { it.memberParts() == listOf("value", "child", "name") }
            .single().projectRecordMemberResolution).isNull()
    }

    private fun scan(
        sources: List<Pair<FileId, String>>,
        observedFile: FileId,
        check: RecordingCheck
    ): com.felipebz.zpa.squid.AstScannerResult {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } }, concurrent = false
        ))
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        var result: com.felipebz.zpa.squid.AstScannerResult? = null
        sources.forEach { (fileId, source) ->
            val scanned = scanner.scanFile(
                FixtureFile(fileId, source),
                if (fileId == observedFile) listOf(check) else emptyList(),
                fileId
            )
            if (fileId == observedFile) result = scanned
        }
        return result!!
    }

    private class RecordingCheck : PlSqlCheck() {
        val members = mutableListOf<SemanticAstNode>()

        init {
            subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
        }

        override fun visitNode(node: AstNode) {
            members += node as SemanticAstNode
        }
    }

    private fun SemanticAstNode.memberParts(): List<String> =
        getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME).map { it.tokenOriginalValue }

    private class FixtureFile(
        private val id: FileId,
        private val source: String
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = id.value
        override fun path(): Path = Path.of(id.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
