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
package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.ProjectRecordFieldTypeResolutionVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProjectRecordFieldTypeResolutionSemanticIntegrationTest {
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun resolvesAQualifiedNamedFieldTypeToTheExactProjectDeclaration() {
        val customerFile = FileId("customer.sql")
        val orderFile = FileId("order.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val sources = listOf(
            customerFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (name VARCHAR2(100)); END customer_pkg;",
            orderFile to "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;",
            useFile to "DECLARE value order_pkg.order_rec; BEGIN value.customer := NULL; END;"
        )

        val scanned = scan(sources, useFile, check)
        val member = check.member(listOf("value", "customer"))
        val memberResolution = member.projectRecordMemberResolution as ProjectRecordMemberResolution.Resolved
        val fieldType = member.projectRecordFieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named
        val expected = scanned.index.findTypes(
            name("customer_pkg"),
            OracleIdentifier.fromSource("customer_rec")
        ).single()

        assertThat(fieldType.field).isSameAs(memberResolution.field)
        assertThat(fieldType.field.typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat(fieldType.resolution).isEqualTo(
            ProjectTypeResolution.Resolved(fieldType.field.typeRef as NamedTypeRef, expected)
        )
        assertThat((fieldType.resolution as ProjectTypeResolution.Resolved).declaration).isSameAs(expected)
        assertThat(scanned.result.symbols.single { it.name.equals("value", true) }.datatype)
            .isSameAs(UnknownDatatype)
    }

    @Test
    fun resolvesAnUnqualifiedFieldTypeUsingItsContainingPackageOwner() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val sources = listOf(
            packageFile to """
                CREATE PACKAGE p AS
                  TYPE child_t IS RECORD (id NUMBER);
                  TYPE parent_t IS RECORD (child child_t);
                END p;
            """.trimIndent(),
            useFile to "DECLARE value p.parent_t; BEGIN value.child := NULL; END;"
        )

        val scanned = scan(sources, useFile, check)
        val fieldType = check.member(listOf("value", "child"))
            .projectRecordFieldTypeResolution as ProjectRecordFieldTypeResolution.Named
        val expected = scanned.index.findTypes(
            name("p"),
            OracleIdentifier.fromSource("child_t")
        ).single()

        assertThat((fieldType.resolution as ProjectTypeResolution.Resolved).declaration).isSameAs(expected)
    }

    @Test
    fun fieldTypeResolutionIsIndependentOfFileOrder() {
        val customerFile = FileId("customer.sql")
        val orderFile = FileId("order.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            customerFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (name VARCHAR2(100)); END customer_pkg;",
            orderFile to "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;",
            useFile to "DECLARE value order_pkg.order_rec; BEGIN value.customer := NULL; END;"
        )
        val forward = scan(sources, useFile, RecordingCheck())
        val reverse = scan(sources.asReversed(), useFile, RecordingCheck())

        val forwardResolution = forward.fieldTypeResolution(listOf("value", "customer"))
        val reverseResolution = reverse.fieldTypeResolution(listOf("value", "customer"))

        assertThat(forwardResolution).isEqualTo(reverseResolution)
        assertThat((forwardResolution as ProjectRecordFieldTypeResolution.Named).resolution)
            .isEqualTo((reverseResolution as ProjectRecordFieldTypeResolution.Named).resolution)
    }

    @Test
    fun preservesAmbiguousFieldTypeResolutionWithoutSelectingACandidate() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val orderFile = FileId("order.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            firstFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (id NUMBER); END customer_pkg;",
            secondFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (id NUMBER); END customer_pkg;",
            orderFile to "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;",
            useFile to "DECLARE value order_pkg.order_rec; BEGIN value.customer := NULL; END;"
        ), useFile, check)

        val resolution = (check.member(listOf("value", "customer"))
            .projectRecordFieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
        assertThat((resolution as ProjectTypeResolution.Ambiguous).candidates.map { it.fileId.value })
            .containsExactly("first.sql", "second.sql")
        assertThat(scanned.result.issues).isEmpty()
    }

    @Test
    fun preservesIncompleteFieldTypeResolution() {
        val knownFile = FileId("known.sql")
        val orderFile = FileId("order.sql")
        val brokenFile = FileId("broken.sql")
        val useFile = FileId("use.sql")
        val sourceExtractor = ProjectDeclarationExtractor()
        val preparation = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { fileId, source ->
            if (fileId == brokenFile) error("declaration preparation failed")
            sourceExtractor.extract(fileId, source)
        })
        val sources = listOf(
            ProjectSource(knownFile) { "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (id NUMBER); END customer_pkg;" },
            ProjectSource(orderFile) { "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;" },
            ProjectSource(brokenFile) { "ignored" },
            ProjectSource(useFile) { "DECLARE value order_pkg.order_rec; BEGIN value.customer := NULL; END;" }
        )
        val context = ProjectAnalysisContext.prepared(preparation.prepare(sources, concurrent = false))
        val orderType = sourceExtractor.extract(orderFile, sources[1].contents())
            .filterIsInstance<PackageTypeDeclaration>().single()
        val field = orderType.recordFields.single()
        val memberNode = SemanticAstNode(PlSqlGrammar.MEMBER_EXPRESSION, "MEMBER_EXPRESSION", null)
        memberNode.projectRecordMemberResolution = ProjectRecordMemberResolution.Resolved(orderType, field)
        val visitor = ProjectRecordFieldTypeResolutionVisitor(ProjectTypeResolver(context))
        visitor.visitNode(memberNode)

        val resolution = (memberNode.projectRecordFieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named).resolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat((resolution as ProjectTypeResolution.IncompleteIndex).knownCandidates).hasSize(1)
    }

    @Test
    fun preservesNotPreparedFieldTypeResolution() {
        val declaration = extractor.extract(
            FileId("record.sql"),
            "CREATE PACKAGE p AS TYPE t IS RECORD (child child_type); END p;"
        ).filterIsInstance<PackageTypeDeclaration>().single()
        val node = SemanticAstNode(PlSqlGrammar.MEMBER_EXPRESSION, "MEMBER_EXPRESSION", null)
        node.projectRecordMemberResolution = ProjectRecordMemberResolution.Resolved(
            declaration,
            declaration.recordFields.single()
        )

        ProjectRecordFieldTypeResolutionVisitor(
            ProjectTypeResolver(ProjectAnalysisContext.NOT_PREPARED)
        ).visitNode(node)

        val resolution = (node.projectRecordFieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named).resolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.NotPrepared::class.java)
    }

    @Test
    fun keepsBuiltInAndExternalFieldTypesAsNotFoundInProject() {
        val file = FileId("types.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            file to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, external_value external_schema.external_type); END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; value.external_value := NULL; END;"
        ), useFile, check)

        val resolutions = listOf("id", "external_value").map {
            (check.member(listOf("value", it)).projectRecordFieldTypeResolution
                as ProjectRecordFieldTypeResolution.Named).resolution
        }
        assertThat(resolutions).allMatch { it is ProjectTypeResolution.NotFoundInProject }
        assertThat(scanned.result.symbols.single { it.name.equals("value", true) }.datatype)
            .isSameAs(UnknownDatatype)
        assertThat(scanned.result.issues).isEmpty()
    }

    @Test
    fun keepsAnchoredAndRefFieldTypesExplicitlyUnsupported() {
        val declarationFile = FileId("types.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            declarationFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id some_table.id%TYPE, reference REF some_object_type); END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; value.reference := NULL; END;"
        ), useFile, check)

        val resolutions = listOf("id", "reference").map {
            check.member(listOf("value", it)).projectRecordFieldTypeResolution
        }
        assertThat(resolutions).allMatch { it is ProjectRecordFieldTypeResolution.Unsupported }
        assertThat((resolutions[0] as ProjectRecordFieldTypeResolution.Unsupported).typeRef)
            .isInstanceOf(AnchoredTypeRef::class.java)
        assertThat((resolutions[1] as ProjectRecordFieldTypeResolution.Unsupported).typeRef)
            .isInstanceOf(RefTypeRef::class.java)
    }

    @Test
    fun preservesQuotedNamedTypeIdentity() {
        val declarationFile = FileId("quoted.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            declarationFile to "CREATE PACKAGE \"Customer_Pkg\" AS TYPE \"Customer_Rec\" IS RECORD (customer \"Customer_Pkg\".\"Customer_Rec\"); END \"Customer_Pkg\";",
            useFile to "DECLARE value \"Customer_Pkg\".\"Customer_Rec\"; BEGIN value.customer := NULL; END;"
        ), useFile, check)

        val resolution = (check.member(listOf("value", "customer"))
            .projectRecordFieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution
        val expected = scanned.index.findTypes(
            name("\"Customer_Pkg\""),
            OracleIdentifier.fromSource("\"Customer_Rec\"")
        ).single()
        assertThat((resolution as ProjectTypeResolution.Resolved).declaration).isSameAs(expected)
        assertThat(resolution.reference.name.segments).containsExactly(
            OracleIdentifier.fromSource("\"Customer_Pkg\""),
            OracleIdentifier.fromSource("\"Customer_Rec\"")
        )
        assertThat(scanned.result.issues).isEmpty()
    }

    @Test
    fun doesNotAttemptFieldTypeResolutionWhenMemberIsMissing() {
        val declarationFile = FileId("record.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            declarationFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to "DECLARE value p.t; BEGIN value.missing := 1; END;"
        ), useFile, check)

        val member = check.member(listOf("value", "missing"))
        assertThat(member.projectRecordMemberResolution)
            .isEqualTo(ProjectRecordMemberResolution.NotFound(
                extractor.extract(declarationFile, "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;")
                    .filterIsInstance<PackageTypeDeclaration>().single()
            ))
        assertThat(member.projectRecordFieldTypeResolution).isNull()
    }

    @Test
    fun doesNotResolveASecondHopUsingTheFirstFieldType() {
        val declarationFile = FileId("record.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            declarationFile to "CREATE PACKAGE p AS TYPE t IS RECORD (child child_pkg.child_type); END p;",
            useFile to "DECLARE value p.t; BEGIN value.child.name := 1; END;"
        ), useFile, check)

        val member = check.members.single {
            it.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
                .map { child -> child.tokenOriginalValue } == listOf("value", "child", "name")
        }
        assertThat(member.projectRecordMemberResolution).isNull()
        assertThat(member.projectRecordFieldTypeResolution).isNull()
    }

    private fun scan(
        sources: List<Pair<FileId, String>>,
        observedFile: FileId,
        check: RecordingCheck
    ): ScannedFile {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
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
        return ScannedFile(context, result!!, check)
    }

    private data class ScannedFile(
        val context: ProjectAnalysisContext,
        val result: com.felipebz.zpa.squid.AstScannerResult,
        val check: RecordingCheck
    ) {
        val index: ProjectSymbolIndex
            get() = (context.state as ProjectAnalysisContext.State.Prepared).result.index

        fun fieldTypeResolution(parts: List<String>): ProjectRecordFieldTypeResolution? =
            check.member(parts).projectRecordFieldTypeResolution
    }

    private class RecordingCheck : PlSqlCheck() {
        val members = mutableListOf<SemanticAstNode>()

        init {
            subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
        }

        override fun visitNode(node: AstNode) {
            members += node as SemanticAstNode
        }

        fun member(parts: List<String>): SemanticAstNode = members.single {
            it.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
                .map { child -> child.tokenOriginalValue } == parts
        }
    }

    private fun name(vararg segments: String) = QualifiedName(segments.map(OracleIdentifier::fromSource))

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
