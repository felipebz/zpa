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
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution.Status
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import com.felipebz.zpa.project.ProjectTypeShape as FactShape

@OptIn(ZpaExperimentalApi::class)
class ProjectNamedTypeResolutionTest {
    private fun prepare(vararg files: Pair<String, String>, failing: Boolean = false): ProjectSnapshot =
        ProjectSnapshots.prepare(
            files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } +
                if (failing) listOf(ProjectSourceInput.ofReader("zz.sql", ProjectSourceReader { throw IllegalStateException("x") }))
                else emptyList(),
            false
        )

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private val fixture = arrayOf(
        "p.sql" to "CREATE PACKAGE p AS TYPE t IS RECORD (a NUMBER); SUBTYPE s IS NUMBER; TYPE \"Q\" IS TABLE OF NUMBER; END p;",
        "o.sql" to "CREATE TYPE obj AS OBJECT (a NUMBER); CREATE TYPE t AS OBJECT (a NUMBER);",
        // No PACKAGE container declaration is required for member lookup.
        "body.sql" to "CREATE PACKAGE BODY nopkg AS END nopkg;"
    )

    @Test
    fun resolvesStandalonePackageAndSubtypeDeclarations() {
        val s = prepare(*fixture)
        val standalone = s.resolveNamedType(n("obj"))
        assertThat(standalone.status).isEqualTo(Status.RESOLVED)
        assertThat(standalone.declaration.get()).isInstanceOf(ProjectShapedTypeView::class.java)
        assertThat(standalone.candidates.single().id).isEqualTo(standalone.declaration.get().id)
        assertThat(standalone.packageOwner).isEmpty
        assertThat(standalone.name).isEqualTo(n("obj"))

        val pkgType = s.resolveNamedType(n("t"), n("p"))
        assertThat(pkgType.status).isEqualTo(Status.RESOLVED)
        assertThat(pkgType.declaration.get()).isInstanceOf(ProjectPackageTypeView::class.java)
        assertThat(pkgType.packageOwner).contains(n("p"))

        val sub = s.resolveNamedType(n("s"), n("p"))
        assertThat(sub.declaration.get()).isInstanceOf(ProjectSubtypeView::class.java)
        assertThat((sub.declaration.get() as ProjectSubtypeView).baseType.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
    }

    @Test
    fun namespaceRulesInferNoOwner() {
        val s = prepare(*fixture)
        // Unqualified without context: standalone namespace only (standalone t, not p.t).
        val bare = s.resolveNamedType(n("t"))
        assertThat(bare.declaration.get()).isInstanceOf(ProjectShapedTypeView::class.java)
        assertThat(bare.declaration.get().kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        // Qualified name overrides supplied context.
        assertThat(s.resolveNamedType(n("p", "t"), n("other")).declaration.get().kind)
            .isEqualTo(ProjectDeclarationKind.PACKAGE_TYPE)
        // A package that declares nothing of the name reaches the standalone type.
        assertThat(s.resolveNamedType(n("obj"), n("p")).declaration.get().kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        // No owner inferred from file identity or from a package that has no spec.
        assertThat(s.resolveNamedType(n("s")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(s.resolveNamedType(n("t"), n("nopkg")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        // Builtin-looking names are ordinary names.
        assertThat(s.resolveNamedType(n("NUMBER")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun quotingStyleDoesNotMatterButQuotedCaseDoes() {
        val s = prepare(*fixture)
        assertThat(s.resolveNamedType(n("\"Q\""), n("p")).status).isEqualTo(Status.RESOLVED)
        assertThat(s.resolveNamedType(n("q"), n("p")).status).isEqualTo(Status.RESOLVED)
        assertThat(s.resolveNamedType(n("\"q\""), n("p")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(s.resolveNamedType(n("T"), n("P")).status).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun duplicatesAreAmbiguousWithDistinctIdsMatchingInventories() {
        val s = prepare("a.sql" to fixture[0].second, "b.sql" to fixture[0].second)
        val r = s.resolveNamedType(n("p", "t"))
        assertThat(r.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(r.declaration).isEmpty
        assertThat(r.candidates.map { it.id }).doesNotHaveDuplicates().hasSize(2)
        val generic = s.findDeclarations(n("p", "t")).declarations
        assertThat(r.candidates.map { it.id }).isEqualTo(generic.map { it.id })
        assertThat(s.declarations.map { it.id }).containsAll(r.candidates.map { it.id })
        assertThat(s.resolveNamedType(n("p", "t")).candidates.map { it.id }).isEqualTo(r.candidates.map { it.id })
    }

    @Test
    fun incompleteIndexTakesPrecedenceForEveryCardinality() {
        val zero = prepare(fixture[1], failing = true).resolveNamedType(n("missing"))
        assertThat(zero.status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(zero.candidates).isEmpty()
        assertThat(zero.failures).hasSize(1)
        assertThat(zero.failures.single().fileId).isEqualTo("zz.sql")

        val one = prepare(fixture[1], failing = true).resolveNamedType(n("obj"))
        assertThat(one.status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(one.candidates).hasSize(1)
        assertThat(one.declaration).isEmpty

        val many = prepare("a.sql" to fixture[0].second, "b.sql" to fixture[0].second, failing = true)
            .resolveNamedType(n("p", "t"))
        assertThat(many.status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(many.candidates).hasSize(2)
    }

    @Test
    fun resultCollectionsRejectMutationAndSnapshotsScopeIds() {
        val s1 = prepare("a.sql" to fixture[0].second, "b.sql" to fixture[0].second, failing = true)
        val r = s1.resolveNamedType(n("p", "t"))
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (r.candidates as MutableList<ProjectTypeDeclarationView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (r.failures as MutableList<ProjectPreparationFailure>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)

        val a = prepare(*fixture).resolveNamedType(n("obj")).declaration.get()
        val b = prepare(*fixture).resolveNamedType(n("obj")).declaration.get()
        assertThat(a.id).isNotEqualTo(b.id)
        assertThat(a.id.ordinal).isEqualTo(b.id.ordinal)
    }

    @Test
    fun repeatedFactReferencesStayDistinctOccurrencesAndNotPreparedIsInternal() {
        val file = FileId("r.sql")
        val range = SourceRange(file, 1, 0, 1, 5)
        val fact = PackageTypeDeclaration(
            QualifiedName(listOf(OracleIdentifier.fromSource("p"))), OracleIdentifier.fromSource("t"),
            FactShape.RECORD, emptyList<ProjectRecordField>(), file, range
        )
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(fact, fact))
        val result = ProjectIndexPreparationResult(builder.build(), 1, emptyList())
        val snapshot = ProjectSnapshot.create(result, ProjectPreparationState.PREPARED_WITH_DECLARATIONS, emptyList(), listOf("r.sql"))
        val r = snapshot.resolveNamedType(n("p", "t"))
        assertThat(r.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(r.candidates.map { it.id.ordinal }).containsExactly(0, 1)

        val empty = ProjectIndexPreparationResult(ProjectSymbolIndex.empty(), 0, emptyList())
        val unprepared = ProjectSnapshot.create(empty, ProjectPreparationState.NOT_PREPARED, emptyList(), emptyList())
        val u = unprepared.resolveNamedType(n("t"), n("p"))
        assertThat(u.status).isEqualTo(Status.NOT_PREPARED)
        assertThat(u.candidates).isEmpty()
        assertThat(u.packageOwner).contains(n("p"))
    }
}
