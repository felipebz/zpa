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

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Optional

@OptIn(ZpaExperimentalApi::class)
class ProjectTypeViewsTest {
    private fun prepare(vararg files: Pair<String, String>, extra: List<ProjectSourceInput> = emptyList()): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + extra, false)

    private val pkg = "CREATE PACKAGE p AS\n" +
        "  TYPE rec_t IS RECORD (id NUMBER, \"Mixed\" emp.sal%TYPE, row_c emp%ROWTYPE, o REF obj_t, q other.t);\n" +
        "  TYPE rows_t IS TABLE OF NUMBER;\n" +
        "  TYPE cur_t IS REF CURSOR;\n" +
        "  SUBTYPE small IS q.base_t;\n" +
        "  SUBTYPE anch IS emp.sal%TYPE;\n" +
        "END p;"

    @Test
    fun packageTypesExposeOwnerLocalNameShapeAndOrderedFields() {
        val snapshot = prepare("p.sql" to pkg)
        val rec = snapshot.findDeclarations(ProjectQualifiedName.of("p", "rec_t")).declarations.single()
        rec as ProjectPackageTypeView
        assertThat(rec.shape).isEqualTo(Optional.of(ProjectTypeShape.RECORD))
        assertThat(rec.owner).isEqualTo(ProjectQualifiedName.of("p"))
        assertThat(rec.localName).isEqualTo(ProjectIdentifier.fromSource("REC_T"))
        assertThat(rec.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_TYPE)
        val fields = rec.recordFields
        assertThat(fields.map { it.ordinal }).containsExactly(0, 1, 2, 3, 4)
        assertThat(fields.map { it.name.originalSpelling }).containsExactly("id", "\"Mixed\"", "row_c", "o", "q")
        assertThat(fields[1].name.isQuoted).isTrue()
        assertThat(fields.map { it.typeRef.kind }).containsExactly(
            ProjectTypeReference.Kind.NAMED, ProjectTypeReference.Kind.ANCHORED, ProjectTypeReference.Kind.ANCHORED,
            ProjectTypeReference.Kind.REF, ProjectTypeReference.Kind.NAMED
        )
        assertThat(fields[1].typeRef.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.TYPE))
        assertThat(fields[2].typeRef.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.ROWTYPE))
        assertThat(fields[0].typeRef.anchor).isEmpty
        assertThat(fields[3].typeRef.anchor).isEmpty
        assertThat(fields[1].typeRef.name).isEqualTo(ProjectQualifiedName.of("emp", "sal"))
        assertThat(fields[3].typeRef.name).isEqualTo(ProjectQualifiedName.of("obj_t"))
        assertThat(fields[4].typeRef.name).isEqualTo(ProjectQualifiedName.of("other", "t"))
        assertThat(fields[0].sourceRange.fileId).isEqualTo("p.sql")
        assertThat(fields[0].sourceRange.startLine).isEqualTo(2)
        assertThat(fields[1].sourceRange.startColumn).isGreaterThan(fields[0].sourceRange.startColumn)
        assertThat(fields[0].typeRef.sourceRange.startLine).isEqualTo(2)
    }

    @Test
    fun nonRecordPackageTypesHaveNoFieldsAndShapesMap() {
        val snapshot = prepare("p.sql" to pkg)
        fun type(n: String) = snapshot.findDeclarations(ProjectQualifiedName.of("p", n)).declarations.single() as ProjectPackageTypeView
        assertThat(type("rows_t").shape).isEqualTo(Optional.of(ProjectTypeShape.COLLECTION))
        assertThat(type("rows_t").recordFields).isEmpty()
        assertThat(type("cur_t").shape).isEqualTo(Optional.of(ProjectTypeShape.REF_CURSOR))
        assertThat(type("cur_t").recordFields).isEmpty()
    }

    @Test
    fun standaloneTypeShapeAndMissingShape() {
        val snapshot = prepare(
            "o.sql" to "CREATE TYPE o_t AS OBJECT (id NUMBER);",
            "c.sql" to "CREATE TYPE c_t AS TABLE OF NUMBER;",
            "x.sql" to "CREATE TYPE x_t;"
        )
        fun shape(n: String): Optional<ProjectTypeShape> =
            (snapshot.findDeclarations(ProjectQualifiedName.of(n)).declarations.single() as ProjectShapedTypeView).shape
        assertThat(shape("o_t")).isEqualTo(Optional.of(ProjectTypeShape.OBJECT))
        assertThat(shape("c_t")).isEqualTo(Optional.of(ProjectTypeShape.COLLECTION))
        assertThat(shape("x_t")).isEmpty
        val o = snapshot.findDeclarations(ProjectQualifiedName.of("o_t")).declarations.single()
        assertThat(o).isNotInstanceOf(ProjectPackageTypeView::class.java)
        assertThat(o.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
    }

    @Test
    fun subtypeBaseReferencesStaySyntax() {
        val snapshot = prepare("p.sql" to pkg)
        fun sub(n: String) = snapshot.findDeclarations(ProjectQualifiedName.of("p", n)).declarations.single() as ProjectSubtypeView
        val small = sub("small")
        assertThat(small.owner).isEqualTo(ProjectQualifiedName.of("p"))
        assertThat(small.localName).isEqualTo(ProjectIdentifier.fromSource("small"))
        assertThat(small.baseType.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(small.baseType.name).isEqualTo(ProjectQualifiedName.of("q", "base_t"))
        assertThat(small.baseType.anchor).isEmpty
        assertThat(sub("anch").baseType.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.TYPE))
        assertThat(small).isNotInstanceOf(ProjectShapedTypeView::class.java)
    }

    @Test
    fun specializedViewsKeepEquivalentIdsAcrossInventoriesAndOrdering() {
        val snapshot = prepare("p.sql" to pkg, "dup.sql" to pkg, extra = listOf(
            ProjectSourceInput.ofReader("z.sql", ProjectSourceReader { throw IllegalStateException("x") })
        ))
        val all = snapshot.declarations
        val byId = all.associateBy { it.id }
        val named = snapshot.findDeclarations(ProjectQualifiedName.of("p", "rec_t"))
        assertThat(named.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(named.declarations).hasSize(2)
        assertThat(named.declarations.map { it.sourceRange.fileId }).containsExactly("dup.sql", "p.sql")
        named.declarations.forEach {
            assertThat(it).isInstanceOf(ProjectPackageTypeView::class.java)
            assertThat(byId[it.id]).isInstanceOf(ProjectPackageTypeView::class.java)
            assertThat(byId[it.id]!!.qualifiedName).isEqualTo(it.qualifiedName)
        }
        val perFile = snapshot.declarationsFor("p.sql").declarations
        assertThat(perFile.map { it.id }).isEqualTo(all.filter { it.fileId == "p.sql" }.map { it.id })
        assertThat(perFile.filterIsInstance<ProjectSubtypeView>()).hasSize(2)
    }

    @Test
    fun typeViewCollectionsRejectMutation() {
        val snapshot = prepare("p.sql" to pkg)
        val rec = snapshot.findDeclarations(ProjectQualifiedName.of("p", "rec_t")).declarations.single() as ProjectPackageTypeView
        @Suppress("UNCHECKED_CAST")
        val fields = rec.recordFields as MutableList<ProjectRecordFieldView>
        assertThatThrownBy { fields.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { fields.add(fields[0]) }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(rec.recordFields).hasSize(5)
    }
}
