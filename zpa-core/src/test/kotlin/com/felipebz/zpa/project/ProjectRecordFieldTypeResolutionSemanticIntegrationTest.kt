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
import com.felipebz.zpa.api.symbols.PlSqlType
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
        val fieldType = member.projectRecordFieldTypeResolution!!
        val expected = scanned.index.findTypes(
            name("customer_pkg"),
            OracleIdentifier.fromSource("customer_rec")
        ).single()

        assertThat(fieldType.field).isSameAs(memberResolution.field)
        assertThat(fieldType.field.typeRef).isInstanceOf(NamedTypeRef::class.java)
        val projectResolution = fieldType.resolution as TypeRefSemanticResolution.Project
        assertThat(projectResolution.resolution).isEqualTo(
            ProjectTypeResolution.Resolved(fieldType.field.typeRef as NamedTypeRef, expected)
        )
        assertThat((projectResolution.resolution as ProjectTypeResolution.Resolved).declaration).isSameAs(expected)
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
            .projectRecordFieldTypeResolution!!
        val expected = scanned.index.findTypes(
            name("p"),
            OracleIdentifier.fromSource("child_t")
        ).single()

        assertThat(((fieldType.resolution as TypeRefSemanticResolution.Project).resolution
            as ProjectTypeResolution.Resolved).declaration).isSameAs(expected)
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
        assertThat(forwardResolution!!.resolution)
            .isEqualTo(reverseResolution!!.resolution)
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
            .projectRecordFieldTypeResolution!!.resolution as TypeRefSemanticResolution.Project).resolution
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
        val visitor = ProjectRecordFieldTypeResolutionVisitor(
            ProjectRecordFieldTypeResolver(TypeRefSemanticResolver(ProjectTypeResolver(context)))
        )
        visitor.visitNode(memberNode)

        val resolution = (memberNode.projectRecordFieldTypeResolution!!.resolution
            as TypeRefSemanticResolution.Project).resolution
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
            ProjectRecordFieldTypeResolver(
                TypeRefSemanticResolver(ProjectTypeResolver(ProjectAnalysisContext.NOT_PREPARED))
            )
        ).visitNode(node)

        val resolution = (node.projectRecordFieldTypeResolution!!.resolution
            as TypeRefSemanticResolution.Project).resolution
        assertThat(resolution).isInstanceOf(ProjectTypeResolution.NotPrepared::class.java)
    }

    @Test
    fun distinguishesBuiltInAndExternalFieldTypes() {
        val file = FileId("types.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            file to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, external_value external_schema.external_type); END p;",
            useFile to "DECLARE value p.t; BEGIN value.id := 1; value.external_value := NULL; END;"
        ), useFile, check)

        val resolutions = listOf("id", "external_value").map {
            check.member(listOf("value", it)).projectRecordFieldTypeResolution!!.resolution
        }
        assertThat(resolutions[0]).isEqualTo(
            TypeRefSemanticResolution.BuiltIn(
                (check.member(listOf("value", "id")).projectRecordFieldTypeResolution!!.field.typeRef
                    as NamedTypeRef),
                PlSqlType.NUMERIC
            )
        )
        assertThat((resolutions[1] as TypeRefSemanticResolution.Project).resolution)
            .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
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
        assertThat(resolutions).allMatch { it?.resolution is TypeRefSemanticResolution.Unsupported }
        assertThat((resolutions[0]!!.resolution as TypeRefSemanticResolution.Unsupported).reference)
            .isInstanceOf(AnchoredTypeRef::class.java)
        assertThat((resolutions[1]!!.resolution as TypeRefSemanticResolution.Unsupported).reference)
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
            .projectRecordFieldTypeResolution!!.resolution as TypeRefSemanticResolution.Project).resolution
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
    fun doesNotAttachDirectMemberMetadataToAnUnresolvedMultiHopExpression() {
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

    @Test
    fun resolvesExactlyTwoProjectRecordHopsInReadAndWriteExpressions() {
        val customerFile = FileId("customer.sql")
        val orderFile = FileId("order.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val sources = listOf(
            customerFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (name VARCHAR2(100)); END customer_pkg;",
            orderFile to "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;",
            useFile to "DECLARE value order_pkg.order_rec; result VARCHAR2(100); BEGIN value.customer.name := 'x'; result := value.customer.name; END;"
        )

        val scanned = scan(sources, useFile, check)
        val orderType = scanned.index.findTypes(name("order_pkg"), OracleIdentifier.fromSource("order_rec"))
            .single() as PackageTypeDeclaration
        val customerType = scanned.index.findTypes(name("customer_pkg"), OracleIdentifier.fromSource("customer_rec"))
            .single() as PackageTypeDeclaration
        val firstField = orderType.recordFields.single()
        val secondField = customerType.recordFields.single()
        val paths = check.members.filter { it.parts() == listOf("value", "customer", "name") }

        assertThat(paths).hasSize(2)
        paths.forEach { pathNode ->
            val path = pathNode.projectRecordMemberPathResolution
            assertThat(path).isInstanceOf(ProjectRecordMemberPathResolution.Completed::class.java)
            val completed = path as ProjectRecordMemberPathResolution.Completed
            assertThat(completed.segments).hasSize(2)
            assertThat(completed.segments[0].field).isSameAs(firstField)
            assertThat(completed.segments[1].field).isSameAs(secondField)

            val firstTypeResolution = completed.segments[0].fieldTypeResolution
            assertThat((firstTypeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
                .isEqualTo(ProjectTypeResolution.Resolved(firstField.typeRef as NamedTypeRef, customerType))
            val secondTypeResolution = completed.segments[1].fieldTypeResolution
            assertThat(secondTypeResolution.resolution).isEqualTo(
                TypeRefSemanticResolution.BuiltIn(secondField.typeRef as NamedTypeRef, PlSqlType.CHARACTER)
            )
            assertThat(pathNode.projectRecordMemberResolution).isNull()
            assertThat(pathNode.projectRecordFieldTypeResolution).isNull()
        }
        assertThat(scanned.result.symbols.single { it.name.equals("value", true) }.projectTypeDeclaration)
            .isSameAs(orderType)
    }

    @Test
    fun resolvesBothPackageLocalFieldTypesAlongAPath() {
        val packageFile = FileId("package.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            packageFile to """
                CREATE PACKAGE p AS
                  TYPE grandchild_t IS RECORD (id NUMBER);
                  TYPE child_t IS RECORD (name grandchild_t);
                  TYPE parent_t IS RECORD (child child_t);
                END p;
            """.trimIndent(),
            useFile to "DECLARE value p.parent_t; BEGIN value.child.name := 'x'; END;"
        ), useFile, check)

        val path = check.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Completed
        val childType = scanned.index.findTypes(name("p"), OracleIdentifier.fromSource("child_t")).single()
        assertThat((path.segments[0].fieldTypeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.Resolved(path.segments[0].field.typeRef as NamedTypeRef, childType))
        assertThat(path.segments[1].field).isSameAs((childType as PackageTypeDeclaration).recordFields.single())
        val grandchildType = scanned.index.findTypes(name("p"), OracleIdentifier.fromSource("grandchild_t")).single()
        assertThat((path.segments[1].fieldTypeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.Resolved(path.segments[1].field.typeRef as NamedTypeRef, grandchildType))
    }

    @Test
    fun memberPathTargetsAreIndependentOfFileOrder() {
        val addressFile = FileId("address.sql")
        val customerFile = FileId("customer.sql")
        val orderFile = FileId("order.sql")
        val useFile = FileId("use.sql")
        val sources = listOf(
            addressFile to "CREATE PACKAGE address_pkg AS TYPE address_rec IS RECORD (city VARCHAR2(100)); END address_pkg;",
            customerFile to "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (address address_pkg.address_rec); END customer_pkg;",
            orderFile to "CREATE PACKAGE order_pkg AS TYPE order_rec IS RECORD (customer customer_pkg.customer_rec); END order_pkg;",
            useFile to "DECLARE value order_pkg.order_rec; BEGIN value.customer.address.city := 'x'; END;"
        )

        val forward = scan(sources, useFile, RecordingCheck())
        val reverse = scan(sources.asReversed(), useFile, RecordingCheck())

        assertThat(forward.check.path(listOf("value", "customer", "address", "city"))
            .projectRecordMemberPathResolution)
            .isEqualTo(reverse.check.path(listOf("value", "customer", "address", "city"))
                .projectRecordMemberPathResolution)
    }

    @Test
    fun preservesQuotedSecondHopFieldIdentity() {
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            childFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (\"Display Name\" VARCHAR2(100)); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;",
            useFile to "DECLARE value parent_pkg.parent_rec; BEGIN value.child.\"Display Name\" := 'x'; END;"
        ), useFile, check)

        val childType = scanned.index.findTypes(name("child_pkg"), OracleIdentifier.fromSource("child_rec"))
            .single() as PackageTypeDeclaration
        val path = check.path(listOf("value", "child", "\"Display Name\""))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Completed

        assertThat(path.segments[1].field).isSameAs(childType.recordFields.single())
        assertThat(path.segments[1].field.name)
            .isEqualTo(OracleIdentifier.fromSource("\"Display Name\""))
        assertThat(path.segments[1].fieldTypeResolution.resolution).isEqualTo(
            TypeRefSemanticResolution.BuiltIn(
                path.segments[1].field.typeRef as NamedTypeRef,
                PlSqlType.CHARACTER
            )
        )
    }

    @Test
    fun stopsBeforeTheSecondHopWhenTheFirstFieldTypeIsAmbiguous() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val parentFile = FileId("parent.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            firstFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (name VARCHAR2(100)); END child_pkg;",
            secondFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (name VARCHAR2(100)); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;",
            useFile to "DECLARE value parent_pkg.parent_rec; BEGIN value.child.name := 'x'; END;"
        ), useFile, check)

        val path = check.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Stopped
        val typeResolution = path.segments.single().fieldTypeResolution.resolution
            as TypeRefSemanticResolution.Project
        assertThat(typeResolution.resolution).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
        assertThat(path.nextMember).isEqualTo(OracleIdentifier.fromSource("name"))
        assertThat(path.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED)
    }

    @Test
    fun stopsWhenTheFirstFieldTypeIsNotInTheProject() {
        val parentFile = FileId("parent.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child external_schema.external_type); END parent_pkg;",
            useFile to "DECLARE value parent_pkg.parent_rec; BEGIN value.child.name := 'x'; END;"
        ), useFile, check)

        val path = check.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Stopped
        val typeResolution = path.segments.single().fieldTypeResolution.resolution
            as TypeRefSemanticResolution.Project
        assertThat(typeResolution.resolution).isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThat(path.segments).hasSize(1)
    }

    @Test
    fun terminalFieldTypeStateDoesNotStopACompletedPath() {
        val childFile = FileId("child.sql")
        val duplicateOne = FileId("duplicate-one.sql")
        val duplicateTwo = FileId("duplicate-two.sql")
        val parentFile = FileId("parent.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            childFile to """
                CREATE PACKAGE child_pkg AS
                  TYPE child_rec IS RECORD (
                    external_value external_schema.external_type,
                    anchored_value some_table.id%TYPE,
                    ref_value REF some_object_type,
                    ambiguous_value duplicate_pkg.duplicate_type
                  );
                END child_pkg;
            """.trimIndent(),
            duplicateOne to "CREATE PACKAGE duplicate_pkg AS TYPE duplicate_type IS RECORD (id NUMBER); END duplicate_pkg;",
            duplicateTwo to "CREATE PACKAGE duplicate_pkg AS TYPE duplicate_type IS RECORD (id NUMBER); END duplicate_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;",
            useFile to "DECLARE value parent_pkg.parent_rec; BEGIN value.child.external_value := NULL; value.child.anchored_value := NULL; value.child.ref_value := NULL; value.child.ambiguous_value := NULL; END;"
        ), useFile, check)

        val childType = scanned.index.findTypes(name("child_pkg"), OracleIdentifier.fromSource("child_rec"))
            .single() as PackageTypeDeclaration
        val expectedFields = childType.recordFields.associateBy { it.name.lookupName }
        listOf("external_value", "anchored_value", "ref_value", "ambiguous_value").forEach { fieldName ->
            val path = check.path(listOf("value", "child", fieldName))
                .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Completed
            val second = path.segments[1]
            assertThat(second.field).isSameAs(expectedFields[fieldName.uppercase()])
            val typeResolution = second.fieldTypeResolution
            when (fieldName) {
                "external_value" -> assertThat((typeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
                    .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
                "ambiguous_value" -> assertThat((typeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
                    .isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
                else -> assertThat(typeResolution.resolution)
                    .isEqualTo(TypeRefSemanticResolution.Unsupported(second.field.typeRef))
            }
        }
    }

    @Test
    fun stopsForUnsupportedFirstFieldTypeFormsAndShapes() {
        val anchoredFile = FileId("anchored.sql")
        val refFile = FileId("ref.sql")
        val collectionFile = FileId("collection.sql")
        val anchoredCheck = RecordingCheck()
        val refCheck = RecordingCheck()
        val collectionCheck = RecordingCheck()

        val anchored = scan(listOf(
            anchoredFile to "CREATE PACKAGE p AS TYPE t IS RECORD (child some_table.row%ROWTYPE); END p;",
            FileId("use_anchored.sql") to "DECLARE value p.t; BEGIN value.child.name := 'x'; END;"
        ), FileId("use_anchored.sql"), anchoredCheck)
        val ref = scan(listOf(
            refFile to "CREATE PACKAGE p AS TYPE t IS RECORD (child REF some_object_type); END p;",
            FileId("use_ref.sql") to "DECLARE value p.t; BEGIN value.child.name := 'x'; END;"
        ), FileId("use_ref.sql"), refCheck)
        scan(listOf(
            collectionFile to "CREATE PACKAGE child_pkg AS TYPE collection_type IS TABLE OF NUMBER; END child_pkg;",
            FileId("parent.sql") to "CREATE PACKAGE p AS TYPE t IS RECORD (child child_pkg.collection_type); END p;",
            FileId("use_collection.sql") to "DECLARE value p.t; BEGIN value.child.name := 'x'; END;"
        ), FileId("use_collection.sql"), collectionCheck)

        val anchoredPath = anchored.check.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Stopped
        val refPath = ref.check.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Stopped
        val collectionPath = collectionCheck.path(listOf("value", "child", "name"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Stopped
        assertThat(anchoredPath.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE)
        assertThat(refPath.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE)
        assertThat(collectionPath.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE)
        assertThat(anchored.result.issues).isEmpty()
    }

    @Test
    fun resolvesThreeProjectRecordMemberHops() {
        val grandchildFile = FileId("grandchild.sql")
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            grandchildFile to "CREATE PACKAGE grandchild_pkg AS TYPE grandchild_rec IS RECORD (c NUMBER); END grandchild_pkg;",
            childFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (b grandchild_pkg.grandchild_rec); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (a child_pkg.child_rec); END parent_pkg;",
            useFile to "DECLARE value parent_pkg.parent_rec; BEGIN value.a.b.c := 1; END;"
        ), useFile, check)

        val path = check.path(listOf("value", "a", "b", "c"))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Completed
        assertThat(path.segments.map { it.field.name.lookupName }).containsExactly("A", "B", "C")
        assertThat(path.segments.map { it.fieldTypeResolution.resolution })
            .allMatch { it is TypeRefSemanticResolution.BuiltIn || it is TypeRefSemanticResolution.Project }
        val childType = scanned.index.findTypes(name("child_pkg"), OracleIdentifier.fromSource("child_rec")).single()
        val grandchildType = scanned.index.findTypes(
            name("grandchild_pkg"),
            OracleIdentifier.fromSource("grandchild_rec")
        ).single()
        assertThat((path.segments[0].fieldTypeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.Resolved(path.segments[0].field.typeRef as NamedTypeRef, childType))
        assertThat((path.segments[1].fieldTypeResolution.resolution as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.Resolved(path.segments[1].field.typeRef as NamedTypeRef, grandchildType))
        assertThat(path.segments[2].fieldTypeResolution.resolution).isEqualTo(
            TypeRefSemanticResolution.BuiltIn(
                path.segments[2].field.typeRef as NamedTypeRef,
                PlSqlType.NUMERIC
            )
        )
    }

    @Test
    fun preservesQuotedMemberIdentityAtADeeperPathPosition() {
        val leafFile = FileId("leaf.sql")
        val middleFile = FileId("middle.sql")
        val rootFile = FileId("root.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        val scanned = scan(listOf(
            leafFile to "CREATE PACKAGE leaf_pkg AS TYPE leaf_rec IS RECORD (\"Display Name\" NUMBER); END leaf_pkg;",
            middleFile to "CREATE PACKAGE middle_pkg AS TYPE middle_rec IS RECORD (leaf leaf_pkg.leaf_rec); END middle_pkg;",
            rootFile to "CREATE PACKAGE root_pkg AS TYPE root_rec IS RECORD (middle middle_pkg.middle_rec); END root_pkg;",
            useFile to "DECLARE value root_pkg.root_rec; BEGIN value.middle.leaf.\"Display Name\" := 1; END;"
        ), useFile, check)

        val leafType = scanned.index.findTypes(name("leaf_pkg"), OracleIdentifier.fromSource("leaf_rec")).single()
        val path = check.path(listOf("value", "middle", "leaf", "\"Display Name\""))
            .projectRecordMemberPathResolution as ProjectRecordMemberPathResolution.Completed

        assertThat(path.segments).hasSize(3)
        assertThat(path.segments.last().field).isSameAs((leafType as PackageTypeDeclaration).recordFields.single())
        assertThat(path.segments.last().field.name)
            .isEqualTo(OracleIdentifier.fromSource("\"Display Name\""))
    }

    @Test
    fun localAndQualifiedNonRecordExpressionsDoNotBecomeProjectPaths() {
        val projectFile = FileId("project.sql")
        val useFile = FileId("use.sql")
        val check = RecordingCheck()
        scan(listOf(
            projectFile to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;",
            useFile to """
                DECLARE
                  TYPE local_t IS RECORD (child local_child_t);
                  value local_t;
                BEGIN
                  value.child.name := 1;
                  p.some_procedure;
                END;
            """.trimIndent()
        ), useFile, check)

        assertThat(check.path(listOf("value", "child", "name")).projectRecordMemberPathResolution).isNull()
        assertThat(check.members.single { it.parts() == listOf("p", "some_procedure") }
            .projectRecordMemberPathResolution).isNull()
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
            it.parts() == parts
        }

        fun path(parts: List<String>): SemanticAstNode = member(parts)
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

private fun SemanticAstNode.parts(): List<String> =
    getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME).map { it.tokenOriginalValue }
