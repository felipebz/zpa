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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProjectRecordMemberPathResolverTest {
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun retainsTwoOrderedExactFieldsAndTypeFactsForBothHops() {
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val context = context(
            childFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (name VARCHAR2(100)); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;"
        )
        val parent = type(context, "parent_pkg", "parent_rec")
        val child = type(context, "child_pkg", "child_rec")
        val resolver = resolver(context)

        val result = resolver.resolve(
            parent,
            listOf(OracleIdentifier.fromSource("child"), OracleIdentifier.fromSource("name"))
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Completed::class.java)
        val completed = result as ProjectRecordMemberPathResolution.Completed
        assertThat(completed.segments.map { it.field }).containsExactly(
            parent.recordFields.single(),
            child.recordFields.single()
        )
        val firstType = completed.segments[0].fieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named
        assertThat((firstType.resolution as ProjectTypeResolution.Resolved).declaration).isSameAs(child)
        val secondType = completed.segments[1].fieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named
        assertThat(secondType.field).isSameAs(child.recordFields.single())
        assertThat(secondType.resolution)
            .isEqualTo(ProjectTypeResolution.NotFoundInProject(secondType.field.typeRef as NamedTypeRef))
        assertThatThrownBy { (completed.segments as MutableList).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun stopsBeforeAnyMemberWhenTheFirstMemberIsMissing() {
        val context = context(
            FileId("record.sql") to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;"
        )

        val result = resolver(context).resolve(
            type(context, "p", "t"),
            listOf("missing", "next").map(OracleIdentifier::fromSource)
        )

        assertThat(result).isEqualTo(
            ProjectRecordMemberPathResolution.Stopped(
                emptyList(),
                OracleIdentifier.fromSource("missing"),
                0,
                ProjectRecordMemberPathResolution.StopReason.MEMBER_NOT_FOUND
            )
        )
    }

    @Test
    fun resolvesAThirdProjectRecordHop() {
        val grandchildFile = FileId("grandchild.sql")
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val context = context(
            grandchildFile to "CREATE PACKAGE grandchild_pkg AS TYPE grandchild_rec IS RECORD (id NUMBER); END grandchild_pkg;",
            childFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (nested grandchild_pkg.grandchild_rec); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;"
        )
        val result = resolver(context).resolve(
            type(context, "parent_pkg", "parent_rec"),
            listOf(
                OracleIdentifier.fromSource("child"),
                OracleIdentifier.fromSource("nested"),
                OracleIdentifier.fromSource("id")
            )
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Completed::class.java)
        val completed = result as ProjectRecordMemberPathResolution.Completed
        assertThat(completed.segments.map { it.field.name.lookupName }).containsExactly("CHILD", "NESTED", "ID")
        assertThat(completed.segments).allMatch { it.fieldTypeResolution is ProjectRecordFieldTypeResolution.Named }
        assertThat((completed.segments[2].fieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution)
            .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
    }

    @Test
    fun preservesTheFirstFieldFactWhenItsTypeCannotEnableTheNextHop() {
        val parentFile = FileId("parent.sql")
        val context = context(
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child external_schema.external_type); END parent_pkg;"
        )
        val result = resolver(context).resolve(
            type(context, "parent_pkg", "parent_rec"),
            listOf(OracleIdentifier.fromSource("child"), OracleIdentifier.fromSource("name"))
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments).hasSize(1)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED)
        assertThat((stopped.segments.single().fieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named).resolution)
            .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
    }

    @Test
    fun preservesAnIncompleteFirstFieldTypeResolutionWithoutResolvingTheNextHop() {
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val brokenFile = FileId("broken.sql")
        val sourceExtractor = ProjectDeclarationExtractor()
        val preparation = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { fileId, source ->
            if (fileId == brokenFile) error("declaration preparation failed")
            sourceExtractor.extract(fileId, source)
        })
        val context = ProjectAnalysisContext.prepared(preparation.prepare(listOf(
            ProjectSource(childFile) { "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (name NUMBER); END child_pkg;" },
            ProjectSource(parentFile) { "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;" },
            ProjectSource(brokenFile) { "ignored" }
        ), concurrent = false))

        val result = resolver(context).resolve(
            type(context, "parent_pkg", "parent_rec"),
            listOf(OracleIdentifier.fromSource("child"), OracleIdentifier.fromSource("name"))
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments).hasSize(1)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED)
        assertThat((stopped.segments.single().fieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named).resolution)
            .isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
    }

    @Test
    fun completedPathRetainsAnIncompleteTerminalFieldType() {
        val file = FileId("record.sql")
        val context = context(
            file to "CREATE PACKAGE p AS TYPE t IS RECORD (child child_t); TYPE child_t IS RECORD (name external_type); END p;"
        )
        val parent = type(context, "p", "t")
        val child = type(context, "p", "child_t")
        val first = ProjectRecordMemberResolution.Resolved(parent, parent.recordFields.single())
        val second = ProjectRecordMemberResolution.Resolved(child, child.recordFields.single())
        val incomplete = ProjectTypeResolution.IncompleteIndex(
            second.field.typeRef as NamedTypeRef,
            emptyList(),
            listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
        )

        val result = ProjectRecordMemberPathResolution.Completed(listOf(
            ProjectRecordMemberPathSegment(
                first.field,
                ProjectRecordFieldTypeResolution.Named(
                    first.field,
                    ProjectTypeResolution.Resolved(first.field.typeRef as NamedTypeRef, child)
                )
            ),
            ProjectRecordMemberPathSegment(
                second.field,
                ProjectRecordFieldTypeResolution.Named(second.field, incomplete)
            )
        ))

        assertThat(result.segments[1].field).isSameAs(second.field)
        assertThat((result.segments[1].fieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution)
            .isSameAs(incomplete)
    }

    @Test
    fun resolvesAPathLongerThanThreeSegments() {
        val typeCount = 9
        val source = (0 until typeCount).joinToString("\n") { index ->
            val fieldName = "field${index + 1}"
            val fieldType = if (index == typeCount - 1) "NUMBER" else "type${index + 1}"
            "TYPE type$index IS RECORD ($fieldName $fieldType);"
        }
        val context = context(FileId("chain.sql") to "CREATE PACKAGE p AS $source END p;")
        val memberNames = (1..typeCount).map { OracleIdentifier.fromSource("field$it") }

        val result = resolver(context).resolve(type(context, "p", "type0"), memberNames)

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Completed::class.java)
        val completed = result as ProjectRecordMemberPathResolution.Completed
        assertThat(completed.segments).hasSize(typeCount)
        assertThat(completed.segments.map { it.field.name.lookupName })
            .containsExactlyElementsOf(memberNames.map { it.lookupName })
        assertThat(completed.segments).allMatch { it.fieldTypeResolution is ProjectRecordFieldTypeResolution.Named }
        assertThat((completed.segments.last().fieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution)
            .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThatThrownBy {
            ProjectRecordMemberPathResolution.Completed(completed.segments.take(1))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun stopsAfterADeeperNotFoundTypeAndRetainsTheResolvedPrefix() {
        val context = context(FileId("chain.sql") to """
            CREATE PACKAGE p AS
              TYPE leaf_t IS RECORD (value external_schema.external_type);
              TYPE middle_t IS RECORD (leaf leaf_t);
              TYPE root_t IS RECORD (middle middle_t);
            END p;
        """.trimIndent())

        val result = resolver(context).resolve(
            type(context, "p", "root_t"),
            listOf(
                OracleIdentifier.fromSource("middle"),
                OracleIdentifier.fromSource("leaf"),
                OracleIdentifier.fromSource("value"),
                OracleIdentifier.fromSource("missing")
            )
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments.map { it.field.name.lookupName })
            .containsExactly("MIDDLE", "LEAF", "VALUE")
        assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("missing"))
        assertThat(stopped.nextMemberOrdinal).isEqualTo(3)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED)
        assertThat((stopped.segments.last().fieldTypeResolution as ProjectRecordFieldTypeResolution.Named).resolution)
            .isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
    }

    @Test
    fun stopsAtAMissingMemberAfterAResolvedPrefix() {
        val context = context(FileId("chain.sql") to """
            CREATE PACKAGE p AS
              TYPE leaf_t IS RECORD (present NUMBER);
              TYPE middle_t IS RECORD (leaf leaf_t);
              TYPE root_t IS RECORD (middle middle_t);
            END p;
        """.trimIndent())

        val result = resolver(context).resolve(
            type(context, "p", "root_t"),
            listOf("middle", "leaf", "missing").map(OracleIdentifier::fromSource)
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments.map { it.field.name.lookupName }).containsExactly("MIDDLE", "LEAF")
        assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("missing"))
        assertThat(stopped.nextMemberOrdinal).isEqualTo(2)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.MEMBER_NOT_FOUND)
    }

    @Test
    fun stopsAfterADeeperAmbiguousTypeAndRetainsTheResolvedPrefix() {
        val context = context(
            FileId("first.sql") to "CREATE PACKAGE ambiguous_pkg AS TYPE target_t IS RECORD (id NUMBER); END ambiguous_pkg;",
            FileId("second.sql") to "CREATE PACKAGE ambiguous_pkg AS TYPE target_t IS RECORD (id NUMBER); END ambiguous_pkg;",
            FileId("chain.sql") to """
                CREATE PACKAGE p AS
                  TYPE leaf_t IS RECORD (value ambiguous_pkg.target_t);
                  TYPE middle_t IS RECORD (leaf leaf_t);
                  TYPE root_t IS RECORD (middle middle_t);
                END p;
            """.trimIndent()
        )

        val result = resolver(context).resolve(
            type(context, "p", "root_t"),
            listOf("middle", "leaf", "value", "next").map(OracleIdentifier::fromSource)
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments).hasSize(3)
        assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("next"))
        assertThat(stopped.nextMemberOrdinal).isEqualTo(3)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED)
        val typeResolution = stopped.segments.last().fieldTypeResolution
            as ProjectRecordFieldTypeResolution.Named
        assertThat(typeResolution.resolution).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
    }

    @Test
    fun stopsAfterADeeperAnchoredRefOrNonRecordType() {
        val anchoredContext = context(FileId("anchored.sql") to """
            CREATE PACKAGE p AS
              TYPE leaf_t IS RECORD (value some_table.id%TYPE);
              TYPE middle_t IS RECORD (leaf leaf_t);
              TYPE root_t IS RECORD (middle middle_t);
            END p;
        """.trimIndent())
        val anchored = resolver(anchoredContext).resolve(
            type(anchoredContext, "p", "root_t"),
            listOf("middle", "leaf", "value", "next").map(OracleIdentifier::fromSource)
        )
        val refContext = context(FileId("ref.sql") to """
            CREATE PACKAGE p AS
              TYPE leaf_t IS RECORD (value REF some_object_type);
              TYPE middle_t IS RECORD (leaf leaf_t);
              TYPE root_t IS RECORD (middle middle_t);
            END p;
        """.trimIndent())
        val ref = resolver(refContext).resolve(
            type(refContext, "p", "root_t"),
            listOf("middle", "leaf", "value", "next").map(OracleIdentifier::fromSource)
        )

        val collectionContext = context(
            FileId("collection.sql") to "CREATE PACKAGE collection_pkg AS TYPE collection_t IS TABLE OF NUMBER; END collection_pkg;",
            FileId("chain.sql") to """
                CREATE PACKAGE p AS
                  TYPE leaf_t IS RECORD (value collection_pkg.collection_t);
                  TYPE middle_t IS RECORD (leaf leaf_t);
                  TYPE root_t IS RECORD (middle middle_t);
                END p;
            """.trimIndent()
        )
        val collection = resolver(collectionContext).resolve(
            type(collectionContext, "p", "root_t"),
            listOf("middle", "leaf", "value", "next").map(OracleIdentifier::fromSource)
        )

        listOf(anchored, ref, collection).forEach { result ->
            assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
            val stopped = result as ProjectRecordMemberPathResolution.Stopped
            assertThat(stopped.segments).hasSize(3)
            assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("next"))
            assertThat(stopped.nextMemberOrdinal).isEqualTo(3)
            assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE)
        }
    }

    @Test
    fun stopsWithAnAmbiguousDeeperMemberWithoutChoosingACandidate() {
        val file = FileId("synthetic.sql")
        val owner = QualifiedName(OracleIdentifier.fromSource("p"))
        val nestedName = OracleIdentifier.fromSource("nested_t")
        val nested = PackageTypeDeclaration(
            owner,
            nestedName,
            ProjectTypeShape.RECORD,
            listOf(field("member", 0), field("MEMBER", 1)),
            file,
            range()
        )
        val root = PackageTypeDeclaration(
            owner,
            OracleIdentifier.fromSource("root_t"),
            ProjectTypeShape.RECORD,
            listOf(ProjectRecordField(
                OracleIdentifier.fromSource("nested"),
                0,
                NamedTypeRef(owner.append(nestedName), range()),
                range()
            )),
            file,
            range()
        )
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(root, nested))
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(builder.build(), 1, emptyList())
        )

        val result = resolver(context).resolve(
            root,
            listOf(OracleIdentifier.fromSource("nested"), OracleIdentifier.fromSource("member"), OracleIdentifier.fromSource("next"))
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments).hasSize(1)
        assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("member"))
        assertThat(stopped.nextMemberOrdinal).isEqualTo(1)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.MEMBER_AMBIGUOUS)
        val memberResolution = ProjectRecordMemberResolver().resolve(nested, OracleIdentifier.fromSource("member"))
        assertThat((memberResolution as ProjectRecordMemberResolution.Ambiguous).candidates.map { it.ordinal })
            .containsExactly(0, 1)
    }

    private fun resolver(context: ProjectAnalysisContext) = ProjectRecordMemberPathResolver(
        ProjectRecordMemberResolver(),
        ProjectRecordFieldTypeResolver(ProjectTypeResolver(context))
    )

    private fun context(vararg sources: Pair<FileId, String>) = ProjectAnalysisContext.prepared(
        ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        )
    )

    private fun type(context: ProjectAnalysisContext, owner: String, name: String): PackageTypeDeclaration =
        (context.state as ProjectAnalysisContext.State.Prepared).result.index.findTypes(
            QualifiedName(OracleIdentifier.fromSource(owner)),
            OracleIdentifier.fromSource(name)
        ).single() as PackageTypeDeclaration

    private fun field(name: String, ordinal: Int) = ProjectRecordField(
        OracleIdentifier.fromSource(name), ordinal,
        NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("NUMBER")), range()), range()
    )

    private fun range() = SourceRange(FileId("synthetic.sql"), 1, 0, 1, 1)
}
