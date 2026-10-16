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
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectPackageExceptionCursorViewTest {
    private fun prepare(vararg files: Pair<String, String>): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private val errorPkg = """
        create or replace package error_pkg is
          app_error exception;
          pragma exception_init(app_error, -20999);
          cursor c_pending(p_status number) return send_queue%rowtype;
          cursor c_all is select id from send_queue;
        end;
        /
    """.trimIndent()

    @Test
    fun specificationViewsListExceptionsAndCursors() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val members = s.declarationsFor("error_pkg.pks").declarations.drop(1)
        assertThat(members.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_EXCEPTION, ProjectDeclarationKind.PACKAGE_CURSOR, ProjectDeclarationKind.PACKAGE_CURSOR
        )
        assertThat(members.map { it.role }).containsOnly(ProjectDeclarationRole.SPECIFICATION)

        val exception = members[0] as ProjectPackageExceptionView
        assertThat(exception.owner).isEqualTo(n("error_pkg"))
        assertThat(exception.localName).isEqualTo(ProjectIdentifier.fromSource("APP_ERROR"))
        assertThat(exception.qualifiedName).isEqualTo(n("error_pkg", "app_error"))
        assertThat(exception.sourceRange.startLine).isEqualTo(2)

        val pending = members[1] as ProjectPackageCursorView
        assertThat(pending.owner).isEqualTo(n("error_pkg"))
        assertThat(pending.localName.originalSpelling).isEqualTo("c_pending")
        assertThat(pending.parameters.map { "${it.ordinal} ${it.name} ${it.mode} ${it.typeRef.name}" }).containsExactly("1 p_status IN number")
        val returnType = pending.returnType.get()
        assertThat(returnType.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(returnType.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)
        assertThat(returnType.name).isEqualTo(n("send_queue"))

        val all = members[2] as ProjectPackageCursorView
        assertThat(all.parameters).isEmpty()
        assertThat(all.returnType).isEmpty
        assertThat(s.findDeclarations(n("ERROR_PKG", "C_ALL")).declarations.map { it.id }).containsExactly(all.id)
    }

    @Test
    fun valueResolutionListsButNeverResolvesExceptionsOrCursors() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val exception = s.resolvePackageValue(n("error_pkg", "app_error"), null)
        assertThat(exception.status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(exception.value).isEmpty
        assertThat(exception.candidates.single()).isInstanceOf(ProjectPackageExceptionView::class.java)
        val cursor = s.resolvePackageValue(n("error_pkg", "c_all"), null)
        assertThat(cursor.status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(cursor.candidates.single()).isInstanceOf(ProjectPackageCursorView::class.java)
    }

    @Test
    fun aCursorOfAnotherPackageDoesNotChangeAVariableResolution() {
        val s = prepare(
            "a.pks" to "CREATE PACKAGE a AS g NUMBER; END a;",
            "b.pks" to "CREATE PACKAGE b AS CURSOR g IS SELECT 1 FROM dual; END b;"
        )
        val g = s.resolvePackageValue(n("a", "g"), null)
        assertThat(g.status).isEqualTo(ProjectValueResolution.Status.RESOLVED)
        assertThat(g.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(s.resolvePackageValue(n("b", "g"), null).status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun duplicateExceptionsInTwoFilesAreTwoOccurrencesInStableOrder() {
        val files = arrayOf("b.pks" to "CREATE PACKAGE p AS e EXCEPTION; END p;", "a.pks" to "CREATE PACKAGE p AS e EXCEPTION; END p;")
        val first = prepare(*files).findDeclarations(n("p", "e")).declarations
        val second = prepare(*files.reversedArray()).findDeclarations(n("p", "e")).declarations
        assertThat(first.map { it.fileId }).containsExactly("a.pks", "b.pks")
        assertThat(second.map { it.fileId }).containsExactly("a.pks", "b.pks")
        assertThat(first.map { it.id }).doesNotHaveDuplicates()
    }

    @Test
    fun cursorAndExceptionAnchorsAreSourceCandidates() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("APP").build()
        fun describe(owner: ProjectQualifiedName?, vararg name: String) = s.describeExternalType(
            ProjectWrittenAnchor.of(n(*name), ProjectTypeReference.Anchor.ROWTYPE), owner, metadata, ProjectIdentifier.fromSource("APP")
        )
        // Inside the package, the member captures the anchor: never an external relation, and the member is the evidence.
        val inside = describe(n("error_pkg"), "c_all")
        assertThat(inside.status).isEqualTo(ProjectExternalTypeDescription.Status.NOT_EXTERNAL)
        assertThat(inside.sourceCandidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_CURSOR)
        assertThat(describe(n("error_pkg"), "app_error").sourceCandidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_EXCEPTION)
        // Qualified from outside, the project does not prove the package is in the current schema: the schema-qualified
        // relation reading stays open, with the package and its cursor listed as possible captors.
        val outside = describe(null, "error_pkg", "c_all")
        assertThat(outside.status).isEqualTo(ProjectExternalTypeDescription.Status.INCOMPLETE)
        assertThat(outside.sourceCandidates.map { it.kind })
            .containsExactly(ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_CURSOR)
    }
}
