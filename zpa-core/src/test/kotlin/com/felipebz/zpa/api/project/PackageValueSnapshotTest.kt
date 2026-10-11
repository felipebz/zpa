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

/** Package values extracted from real sources, observed only through the generic snapshot views. */
@OptIn(ZpaExperimentalApi::class)
class PackageValueSnapshotTest {
    private fun prepare(concurrent: Boolean, vararg files: Pair<String, String>): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, concurrent)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private val spec = """
        CREATE PACKAGE app.p AS
          g NUMBER;
          c CONSTANT VARCHAR2(10) := 'x';
          g VARCHAR2(5);
          PROCEDURE g;
          TYPE t IS RECORD (a NUMBER);
        END p;
    """.trimIndent()
    private val body = "CREATE PACKAGE BODY app.p AS priv NUMBER; PROCEDURE g IS loc NUMBER; BEGIN NULL; END; END p;"
    private val standalone = "CREATE FUNCTION x.g RETURN NUMBER IS BEGIN RETURN 1; END;"

    private fun summary(s: ProjectSnapshot) =
        s.declarations.map { listOf(it.kind, it.role, it.qualifiedName, it.fileId, it.sourceRange.startLine, it.sourceRange.startColumn, it.sourceRange.endColumn, it.id.ordinal) }

    @Test
    fun valuesAppearThroughEveryGenericInventoryWithCanonicalIds() {
        val s = prepare(false, "a.sql" to spec, "b.sql" to body)
        assertThat(s.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        val values = s.declarations.filter { it.kind == ProjectDeclarationKind.PACKAGE_VARIABLE || it.kind == ProjectDeclarationKind.PACKAGE_CONSTANT }
        assertThat(values.map { it.qualifiedName.last.originalSpelling to it.kind }).containsExactly(
            "g" to ProjectDeclarationKind.PACKAGE_VARIABLE, "c" to ProjectDeclarationKind.PACKAGE_CONSTANT, "g" to ProjectDeclarationKind.PACKAGE_VARIABLE
        )
        assertThat(values.map { it.role }).containsOnly(ProjectDeclarationRole.SPECIFICATION)
        assertThat(values.map { it.fileId }).containsOnly("a.sql")
        assertThat(values.map { it.qualifiedName }).containsExactly(n("app", "p", "g"), n("app", "p", "c"), n("app", "p", "g"))
        assertThat(values).noneMatch { it is ProjectSubprogramView || it is ProjectTypeDeclarationView }
        // Equal-name values and a same-named procedure are separate occurrences with distinct IDs.
        assertThat(values.map { it.id }).doesNotHaveDuplicates()
        val byName = s.findDeclarations(n("app", "p", "g")).declarations
        assertThat(byName.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_VARIABLE,
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_PROCEDURE
        )
        assertThat(byName.map { it.id }).doesNotHaveDuplicates()
        val perFile = s.declarationsFor("a.sql").declarations
        assertThat(perFile.map { it.id.ordinal }).containsExactly(0, 1, 2, 3, 4, 5)
        assertThat(perFile.map { it.id }).isEqualTo(s.declarations.filter { it.fileId == "a.sql" }.map { it.id })
        assertThat(byName.filter { it.kind == ProjectDeclarationKind.PACKAGE_VARIABLE }.map { it.id })
            .isEqualTo(values.filter { it.qualifiedName.last.originalSpelling == "g" }.map { it.id })
        // Ranges: whole declaration, name through `;`.
        assertThat(values.map { it.sourceRange.startLine to it.sourceRange.startColumn }).containsExactly(2 to 2, 3 to 2, 4 to 2)
    }

    @Test
    fun bodyLocalsAndPackageOnlyQueriesNeverSeeValues() {
        val s = prepare(false, "a.sql" to spec, "b.sql" to body, "c.sql" to standalone)
        assertThat(s.declarationsFor("b.sql").declarations.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE)
        val members = s.findPackageSubprograms(n("app", "p"), ProjectIdentifier.fromSource("g"))
        assertThat(members.declarations.map { it.kind }).containsOnly(ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertThat(members.declarations).hasSize(2)
        assertThat(s.findPackageSubprograms(n("app", "p"), ProjectIdentifier.fromSource("c")).declarations).isEmpty()
        assertThat(s.resolveNamedType(n("g"), n("app", "p")).candidates).isEmpty()
        assertThat(s.resolveNamedType(n("t"), n("app", "p")).candidates).hasSize(1)
        val value = s.declarations.first { it.kind == ProjectDeclarationKind.PACKAGE_VARIABLE }
        assertThatThrownBy { s.correlateSpecification(value.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun aStandaloneFunctionWithTheSameTextualNameRemainsItsOwnDeclaration() {
        val s = prepare(false, "a.sql" to "CREATE PACKAGE x AS g NUMBER; END x;", "c.sql" to standalone)
        assertThat(s.findDeclarations(n("x", "g")).declarations.map { it.kind to it.fileId }).containsExactly(
            ProjectDeclarationKind.PACKAGE_VARIABLE to "a.sql", ProjectDeclarationKind.STANDALONE_FUNCTION to "c.sql"
        )
    }

    @Test
    fun serialAndConcurrentPreparationAndInputOrderAgree() {
        val files = (1..40).map { "f$it.sql" to "CREATE PACKAGE p$it AS g NUMBER := CASE WHEN 1=1 THEN $it END; c CONSTANT t.x%TYPE := 1; PROCEDURE q; END p$it;" }
        val serial = prepare(false, *files.toTypedArray())
        val concurrent = prepare(true, *files.toTypedArray())
        val reversed = prepare(true, *files.asReversed().toTypedArray())
        assertThat(summary(concurrent)).isEqualTo(summary(serial))
        assertThat(summary(reversed)).isEqualTo(summary(serial))
        assertThat(serial.declarations.count { it.kind == ProjectDeclarationKind.PACKAGE_VARIABLE }).isEqualTo(40)
        assertThat(serial.declarations.count { it.kind == ProjectDeclarationKind.PACKAGE_CONSTANT }).isEqualTo(40)
    }

    @Test
    fun snapshotsAreIndependentAndInventoriesAreUnmodifiable() {
        val a = prepare(false, "a.sql" to spec)
        val b = prepare(false, "a.sql" to spec)
        assertThat(a.declarations.map { it.id }).doesNotContainAnyElementsOf(b.declarations.map { it.id })
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (a.declarations as MutableList<Any?>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (a.findDeclarations(n("app", "p", "g")).declarations as MutableList<Any?>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }
}
