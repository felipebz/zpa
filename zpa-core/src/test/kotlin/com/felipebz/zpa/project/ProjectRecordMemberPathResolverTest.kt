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
    fun retainsTwoOrderedExactFieldsAndTheTypeFactUsedForTheSecondHop() {
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
        assertThat(completed.segments[1].fieldTypeResolution).isNull()
        assertThatThrownBy { (completed.segments as MutableList).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun recordsTheExplicitThirdHopBoundary() {
        val childFile = FileId("child.sql")
        val parentFile = FileId("parent.sql")
        val context = context(
            childFile to "CREATE PACKAGE child_pkg AS TYPE child_rec IS RECORD (name NUMBER); END child_pkg;",
            parentFile to "CREATE PACKAGE parent_pkg AS TYPE parent_rec IS RECORD (child child_pkg.child_rec); END parent_pkg;"
        )
        val result = resolver(context).resolve(
            type(context, "parent_pkg", "parent_rec"),
            listOf(
                OracleIdentifier.fromSource("child"),
                OracleIdentifier.fromSource("name"),
                OracleIdentifier.fromSource("third")
            )
        )

        assertThat(result).isInstanceOf(ProjectRecordMemberPathResolution.Stopped::class.java)
        val stopped = result as ProjectRecordMemberPathResolution.Stopped
        assertThat(stopped.segments).hasSize(2)
        assertThat(stopped.nextMember).isEqualTo(OracleIdentifier.fromSource("third"))
        assertThat(stopped.nextMemberOrdinal).isEqualTo(2)
        assertThat(stopped.reason).isEqualTo(ProjectRecordMemberPathResolution.StopReason.HOP_LIMIT)
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
}
